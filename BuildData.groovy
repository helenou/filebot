import org.tukaani.xz.*

/* ------------------------------------------------------------------------- */


def sortRegexList(path) {
	def set = new TreeSet(String.CASE_INSENSITIVE_ORDER)
	new File(path).eachLine('UTF-8'){
		// check if regex compiles
		set += java.util.regex.Pattern.compile(it.trim()).pattern()
	}
	def out = set.join('\n').saveAs(path)
	println "${out}\n${out.text}\n"
}


// sort and check shared regex collections
sortRegexList("website/data/release-groups.txt")
sortRegexList("website/data/query-blacklist.txt")
sortRegexList("website/data/exclude-blacklist.txt")
sortRegexList("website/data/series-mappings.txt")
sortRegexList("website/data/add-series-alias.txt")


/* ------------------------------------------------------------------------- */


def reviews = []
new File('reviews.csv').eachLine('UTF-8'){
	def s = it.split(';', 3)
	reviews << [user: s[0], date: s[1], text: s[2].replaceAll(/^\"|\"$/, '').replaceAll(/["]{2}/, '"') ]
}
reviews = reviews.sort{ it.date }

def json = new groovy.json.JsonBuilder()
json.call(reviews as List)
json.toPrettyString().saveAs('website/reviews.json')
println "Reviews: " + reviews.size()


/* ------------------------------------------------------------------------- */


def moviedb_out = new File("website/data/moviedb.txt")
def thetvdb_out = new File("website/data/thetvdb.txt")
def anidb_out   = new File("website/data/anidb.txt")

def pack(file, lines) {
	new File(file.parentFile, file.name + '.xz').withOutputStream{ out ->
		new XZOutputStream(out, new LZMA2Options(LZMA2Options.PRESET_DEFAULT)).withWriter('UTF-8'){ writer ->
			lines.each{ writer.append(it).append('\n') }
		}
	}
	def rows = lines.size()
	def columns = lines.collect{ it.split(/\t/).length }.max()
	println "$file ($rows rows, $columns columns)"
}


/* ------------------------------------------------------------------------- */


def isValidMovieName(s) {
	return (s.normalizePunctuation().length() >= 4) || (s=~ /^[A-Z0-9]/ && s =~ /[\p{Alnum}]{3}/)
}

def getNamePermutations(names) {
	def normalize = { s -> s.toLowerCase().normalizePunctuation() }.memoize()
	def fn1 = { s -> s.replaceAll(/(?i)(^(The|A)\s)|([,]\s(The|A)$)/, '') }
	def fn2 = { s -> s.replaceAll(/\s&\s/, ' and ') }
	def fn3 = { s -> s.replaceAll(/\([^\)]*\)$/, '') }

	def out = names*.trim().unique().collectMany{ original ->
		def simplified = original
		[fn1, fn2, fn3].each{ fn -> simplified = fn(simplified).trim() }
		return [original, simplified]
	}.unique{ normalize(it) }.findAll{ it.length() > 0 }

	out = out.findAll{ it.length() >= 2 && !(it ==~ /[1][0-9][1-9]/) && !(it =~ /^[a-z]/) && it =~ /^[@.\p{L}\p{Digit}]/ } // MUST START WITH UNICODE LETTER
	out = out.findAll{ !MediaDetection.releaseInfo.structureRootPattern.matcher(it).matches() } // IGNORE NAMES THAT OVERLAP WITH MEDIA FOLDER NAMES
	// out = out.findAll{ a -> names.take(1).contains(a) || out.findAll{ b -> normalize(a).startsWith(normalize(b) + ' ') }.size() == 0 } // TRY TO EXCLUDE REDUNDANT SUBSTRING DUPLICATES

	return out
}

def treeSort(list, keyFunction) {
	def sorter = new TreeMap(String.CASE_INSENSITIVE_ORDER)
	list.each{
		sorter.put(keyFunction(it), it)
	}
	return sorter.values()
}

def csv(f, delim, keyIndex, valueIndex) {
	def values = [:]
	if (f.isFile()) {
		f.splitEachLine(delim, 'UTF-8') { line ->
			values.put(line[keyIndex], tryQuietly{ line[valueIndex] })
		}
	}
	return values
}


/* ------------------------------------------------------------------------- */


// BUILD moviedb index
def omdb = []
new File('omdb.txt').eachLine('Windows-1252'){
	def line = it.split(/\t/)
	if (line.length > 11 && line[0] ==~ /\d+/ && line[3] ==~ /\d{4}/) {
		def imdbid = line[1].substring(2).toInteger()
		def name = line[2].replaceAll(/\s+/, ' ').trim()
		def year = line[3].toInteger()
		def runtime = line[5]
		def genres = line[6]
		def rating = tryQuietly{ line[12].toFloat() } ?: 0
		def votes = tryQuietly{ line[13].replaceAll(/\D/, '').toInteger() } ?: 0
		
		if (!(genres =~ /Short/ || votes <= 50 || rating <= 2) && ((year >= 1970 && (runtime =~ /(\d.h)|(\d{3}.min)/ || votes >= 500)) || (year >= 1950 && votes >= 20000))) {
			omdb << [imdbid.pad(7), name, year]
		}
	}
}
omdb = omdb.findAll{ (it[0] as int) <= 9999999 && isValidMovieName(it[1]) }


def tmdb_txt = new File('tmdb.txt')
def tmdb_index = csv(tmdb_txt, '\t', 1, [0..-1])

def tmdb = []
omdb.each{ m ->
	def sync = System.currentTimeMillis()
	if (tmdb_index.containsKey(m[0]) && (sync - tmdb_index[m[0]][0].toLong()) < ((m[2].toInteger() < 2000 ? 360 : 120) * 24 * 60 * 60 * 1000L) ) {
		tmdb << tmdb_index[m[0]]
		return
	}
	try {
		def info = WebServices.TheMovieDB.getMovieInfo("tt${m[0]}", Locale.ENGLISH, true)

		if (info.votes <= 1 || info.rating <= 2)
			throw new FileNotFoundException('Insufficient movie data')

		def names = [info.name, info.originalName] + info.alternativeTitles
		[info?.released?.year, m[2]].findResults{ it?.toInteger() }.unique().each{ y ->
			def row = [sync, m[0].pad(7), info.id.pad(7), y.pad(4)] + names
			println row
			tmdb << row
		}
	} catch(FileNotFoundException e) {
		def row = [sync, m[0].pad(7), 0, m[2], m[1]]
		println row
		tmdb << row
	}
}
tmdb*.join('\t').join('\n').saveAs(tmdb_txt)

movies = tmdb.findResults{
	def ity = it[1..3] // imdb id, tmdb id, year
	def names = getNamePermutations(it[4..-1]).findAll{ isValidMovieName(it) }
	if (ity[0].toInteger() > 0 && ity[1].toInteger() > 0 && names.size() > 0)
		return ity + names
	else
		return null
}
movies = treeSort(movies, { it[3, 2].join(' ') })

// sanity check
if (movies.size() < 20000) { die('Movie index sanity failed:' + movies.size()) }
pack(moviedb_out, movies*.join('\t'))


/* ------------------------------------------------------------------------- */


// BUILD tvdb index
def tvdb_txt = new File('tvdb.txt')
def tvdb = [:]

if (tvdb_txt.exists()) {
	tvdb_txt.eachLine('UTF-8'){
		def line = it.split('\t').toList()
		def names = line.subList(5, line.size())
		tvdb.put(line[1] as Integer, [line[0] as Long, line[1] as Integer, line[2], line[3] as Float, line[4] as Float] + names)
	}
}

def tvdb_updates = [:] as TreeMap
new File('updates_all.xml').eachLine('UTF-8'){
	def m = (it =~ '<Series><id>(\\d+)</id><time>(\\d+)</time></Series>')
	while(m.find()) {
		def id = m.group(1) as Integer
		def time = m.group(2) as Integer
		tvdb_updates[id] = [id: id, time: time]
	}
}

// blacklist crap entries
tvdb_updates.remove(219901)
tvdb_updates.remove(256135)


tvdb_updates.values().each{ update ->
	if (tvdb[update.id] == null || update.time > tvdb[update.id][0]) {
		try {
			retry(2, 60000) {
				def seriesNames = []
				def xml = new XmlSlurper().parse("http://thetvdb.com/api/BA864DEE427E384A/series/${update.id}/en.xml")
				def imdbid = xml.Series.IMDB_ID.text()
				seriesNames += xml.Series.SeriesName.text()
				
				def rating = tryQuietly{ xml.Series.Rating.text().toFloat() }
				def votes = tryQuietly{ xml.Series.RatingCount.text().toFloat() }
				
				// only retrieve additional data for reasonably popular shows
				if (votes >= 5 && rating >= 4) {
					tryLogCatch{
						if (imdbid =~ /tt(\d+)/) {
							seriesNames += OMDb.getMovieDescriptor(new Movie(null, 0, imdbid.match(/tt(\d+)/) as int, -1), Locale.ENGLISH).getName()
						}
					}

					// scrape extra alias titles from webpage (not supported yet by API)
					def jsoup = org.jsoup.Jsoup.connect("http://thetvdb.com/?tab=series&id=${update.id}").get()
					def akaseries = jsoup.select('#akaseries table tr table tr')
												.findAll{ it.select('td').any{ it.text() ==~ /en/ } }
												.findResults{ it.select('td').first().text() }
												.findAll{ it?.length() > 0 }
					def intlseries = jsoup.select('#seriesform input')
												.findAll{ it.attr('name') =~ /SeriesName/ }
												.sort{ it.attr('name').match(/\d+/) as int }
												.collect{ it.attr('value') }
												.findAll{ it?.length() > 0 }
					
					println "Scraped data $akaseries and $intlseries for series $seriesNames"
					seriesNames += akaseries
					seriesNames += intlseries
				}

				def data = [update.time, update.id, imdbid, rating ?: 0, votes ?: 0] + seriesNames.findAll{ it != null && it.length() > 0 }
				tvdb.put(update.id, data)
				println "Update $update => $data"
			}
		}
		catch(Throwable e) {
			printException(e, false)
			def data = [update.time, update.id, '', 0, 0]
			tvdb.put(update.id, data)
			println "Update $update => $data"
		}
	}
}

// remove entries that have become invalid
tvdb.keySet().toList().each{ id ->
	if (tvdb_updates[id] == null) {
		println "Invalid ID found: ${tvdb[id]}"
		tvdb.remove(id)
	}
}
tvdb.values().findResults{ it.collect{ it.toString().replace('\t', '').trim() }.join('\t') }.join('\n').saveAs(tvdb_txt)


def thetvdb_index = []
tvdb.values().each{ r ->
	def tvdb_id = r[1]
	def rating = r[3]
	def votes = r[4]
	def names = r.subList(5, r.size())
	
	if ((votes >= 5 && rating >= 4) || (votes >= 2 && rating >= 7) || (votes >= 1 && rating >= 10)) {
		getNamePermutations(names).each{ n ->
			thetvdb_index << [tvdb_id, n]
		}
	}
}

// additional custom mappings
new File('website/data/add-series-alias.txt').splitEachLine(/\t+/, 'UTF-8') { row ->
	def se = thetvdb_index.find{ row[0] == it[1] && !it.contains(row[1]) }
	if (se == null) die("Unabled to find series '${row[0]}': '${row[1]}'")
	thetvdb_index << [se[0], row[1]]
}

thetvdb_index = thetvdb_index.findResults{ [it[0] as Integer, it[1].replaceAll(/\s+/, ' ').trim()] }.findAll{ !(it[1] =~ /(?i:duplicate)/ || it[1] =~ /\d{6,}/ || it[1].startsWith('*') || it[1].endsWith('*') || it[1].length() < 2) }
thetvdb_index = thetvdb_index.sort{ a, b -> a[0] <=> b[0] }

// join and sort
def thetvdb_txt = thetvdb_index.groupBy{ it[0] }.findResults{ k, v -> ([k.pad(6)] + v*.getAt(1).unique{ it.toLowerCase() }).join('\t') }

// sanity check
if (thetvdb_txt.size() < 4000) { die('TheTVDB index sanity failed: ' + thetvdb_txt.size()) }
pack(thetvdb_out, thetvdb_txt)


/* ------------------------------------------------------------------------- */


// BUILD anidb index
def anidb = new AnidbClient('filebot', 5).getAnimeTitles()

def anidb_index = anidb.findResults{
	def names = it.effectiveNames*.replaceAll(/\s+/, ' ')*.trim()*.replaceAll(/['`´‘’ʻ]+/, /'/)
	names = getNamePermutations(names)
	names = names.findAll{ stripReleaseInfo(it)?.length() > 0 }

	return names.empty ? null : [it.getAnimeId().pad(5)] + names.take(4)
}

// join and sort
def anidb_txt = anidb_index.findResults{ row -> row.join('\t') }.sort().unique()

// sanity check
if (anidb_txt.size() < 8000) { die('AniDB index sanity failed:' + anidb_txt.size()) }
pack(anidb_out, anidb_txt)
