
package net.filebot.similarity;


import static org.junit.Assert.*;

import org.junit.Test;


public class DateMetricTest {
	
	private static DateMetric metric = new DateMetric();
	

	@Test
	public void getSimilarity() {
		assertEquals(1, metric.getSimilarity("2008-02-10", "The Daily Show [10.2.2008] Lou Dobbs"), 0);
		assertEquals(0, metric.getSimilarity("2008-01-01", "The Daily Show [10.2.2008] Lou Dobbs"), 0);
		assertEquals(1, metric.getSimilarity("2008-04-03", "The Daily Show - 2008.04.03 - George Clooney"), 0);
		assertEquals(0, metric.getSimilarity("2008-01-01", "The Daily Show - 2008.04.03 - George Clooney"), 0);
	}
	
}
