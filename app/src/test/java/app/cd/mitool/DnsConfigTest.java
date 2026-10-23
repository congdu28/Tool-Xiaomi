package app.cd.mitool;

import app.cd.mitool.util.DnsConfig;
import org.junit.Test;
import static org.junit.Assert.*;

public class DnsConfigTest {
    @Test public void acceptsProviderHostnamesAndNormalizesPastedUrls() {
        String host = DnsConfig.cleanHostname(" HTTPS://DNS.Google:853/dns-query ");
        assertEquals("dns.google", host);
        assertTrue(DnsConfig.isValidHostname(host));
        assertTrue(DnsConfig.isValidHostname("1dot1dot1dot1.cloudflare-dns.com"));
        assertTrue(DnsConfig.isValidHostname("abc123.dns.nextdns.io"));
    }
    @Test public void rejectsIpAddressesAndShellInput() {
        for (String host : new String[]{"", "1.1.1.1", "dns.google;id", "dns.google'", "dns..google",
                "-dns.google", "dns-.google", "dns.google\nwhoami", "$(id).google", "dns_google.com"}) {
            assertFalse(host, DnsConfig.isValidHostname(DnsConfig.cleanHostname(host)));
        }
    }
}
