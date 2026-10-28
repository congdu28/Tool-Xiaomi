package app.cd.mitool;

import app.cd.mitool.ui.FeatureSearch;
import org.junit.Test;
import static org.junit.Assert.*;

public class FeatureSearchTest {
    @Test public void vietnameseQueriesWorkWithoutAccents() {
        assertTrue(FeatureSearch.matches("dong ho", "Màn hình và đồng hồ"));
        assertTrue(FeatureSearch.matches("DO RUNG", "Độ rung"));
        assertTrue(FeatureSearch.matches("thong bao", "Thông báo ứng dụng"));
    }
    @Test public void queriesAreCaseInsensitiveAndWhitespaceTolerant() {
        assertTrue(FeatureSearch.matches("  DNS   RIENG  ", "DNS riêng tư"));
        assertTrue(FeatureSearch.matches("", "Tần số quét"));
        assertTrue(FeatureSearch.matches(null, "Tần số quét"));
    }
    @Test public void allTermsMustMatchTheSameFeature() {
        assertFalse(FeatureSearch.matches("dns dong ho", "DNS riêng tư"));
        assertFalse(FeatureSearch.matches("missing", "Cảnh báo pin"));
    }
}
