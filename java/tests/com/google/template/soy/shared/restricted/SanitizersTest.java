/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.shared.restricted;

import com.google.template.soy.data.SanitizedContent;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.UnsafeSanitizedContentOrdainer;

import junit.framework.TestCase;

public class SanitizersTest extends TestCase {
  private static final String ASCII_CHARS;
  static {
    StringBuilder sb = new StringBuilder(0x80);
    for (int i = 0; i < 0x80; ++i) {
      sb.append((char) i);
    }
    ASCII_CHARS = sb.toString();
  }
  private static final SoyData ASCII_CHARS_SOYDATA = SoyData.createFromExistingData(ASCII_CHARS);

  /** Substrings that might change the parsing mode of scripts they are embedded in. */
  private static final String[] EMBEDDING_HAZARDS = new String[] {
        "</script", "</style", "<!--", "-->", "<![CDATA[", "]]>"
      };

  public final void testEscapeJsString() {
    // The minimal escapes.
    // Do not remove anything from this set without talking to your friendly local security-team@.
    assertEquals(
        "\\x00 \\x22 \\x27 \\\\ \\r \\n \\u2028 \\u2029",
        Sanitizers.escapeJsString("\u0000 \" \' \\ \r \n \u2028 \u2029"));

    for (String hazard : EMBEDDING_HAZARDS) {
      assertFalse(hazard, Sanitizers.escapeJsString(hazard).contains(hazard));
    }

    // Check correctness of other Latins.
    String escapedAscii = (
        "\\x00\u0001\u0002\u0003\u0004\u0005\u0006\u0007\\x08\\t\\n\\x0b\\f\\r\u000e\u000f" +
        "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017" +
        "\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f" +
        " !\\x22#$%\\x26\\x27()*+,-.\\/" +
        "0123456789:;\\x3c\\x3d\\x3e?" +
        "@ABCDEFGHIJKLMNO" +
        "PQRSTUVWXYZ[\\\\]^_" +
        "`abcdefghijklmno" +
        "pqrstuvwxyz{|}~\u007f");
    assertEquals(escapedAscii, Sanitizers.escapeJsString(ASCII_CHARS));
    assertEquals(escapedAscii, Sanitizers.escapeJsString(ASCII_CHARS_SOYDATA));
  }

  public final void testEscapeJsRegExpString() {
    // The minimal escapes.
    // Do not remove anything from this set without talking to your friendly local security-team@.
    assertEquals(
        "\\x00 \\x22 \\x27 \\\\ \\/ \\r \\n \\u2028 \\u2029" +
        // RegExp operators.
        " \\x24\\x5e\\x2a\\x28\\x29\\x2d\\x2b\\x7b\\x7d\\x5b\\x5d\\x7c\\x3f",
        Sanitizers.escapeJsRegex(
            "\u0000 \" \' \\ / \r \n \u2028 \u2029" +
            " $^*()-+{}[]|?"));

    for (String hazard : EMBEDDING_HAZARDS) {
      assertFalse(hazard, Sanitizers.escapeJsRegex(hazard).contains(hazard));
    }

    String escapedAscii = (
        "\\x00\u0001\u0002\u0003\u0004\u0005\u0006\u0007\\x08\\t\\n\\x0b\\f\\r\u000e\u000f" +
        "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017" +
        "\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f" +
        " !\\x22#\\x24%\\x26\\x27\\x28\\x29\\x2a\\x2b\\x2c\\x2d\\x2e\\/" +
        "0123456789\\x3a;\\x3c\\x3d\\x3e\\x3f" +
        "@ABCDEFGHIJKLMNO" +
        "PQRSTUVWXYZ\\x5b\\\\\\x5d\\x5e_" +
        "`abcdefghijklmno" +
        "pqrstuvwxyz\\x7b\\x7c\\x7d~\u007f");
    assertEquals(escapedAscii, Sanitizers.escapeJsRegex(ASCII_CHARS));
    assertEquals(escapedAscii, Sanitizers.escapeJsRegex(ASCII_CHARS_SOYDATA));
  }

  public final void testEscapeJsValue() {
    assertEquals(  // Adds quotes.
        "'Don\\x27t run with \\x22scissors\\x22.\\n'",
        Sanitizers.escapeJsValue("Don't run with \"scissors\".\n"));
    assertEquals(  // SoyData version does the same as String version.
        "'Don\\x27t run with \\x22scissors\\x22.\\n'",
        Sanitizers.escapeJsValue(
            SoyData.createFromExistingData("Don't run with \"scissors\".\n")));
    assertEquals(
        " 4.0 ",
        Sanitizers.escapeJsValue(SoyData.createFromExistingData(4)));
    assertEquals(
        " 4.5 ",
        Sanitizers.escapeJsValue(SoyData.createFromExistingData(4.5)));
    assertEquals(
        " true ",
        Sanitizers.escapeJsValue(SoyData.createFromExistingData(true)));
    assertEquals(
        " null ",
        Sanitizers.escapeJsValue(SoyData.createFromExistingData(null)));
    assertEquals(
        "foo() + bar",
        Sanitizers.escapeJsValue(UnsafeSanitizedContentOrdainer.ordainAsSafe(
            "foo() + bar", SanitizedContent.ContentKind.JS)));
    // Wrong content kind should be wrapped in a string.
    assertEquals(
        "'foo() + bar'",
        Sanitizers.escapeJsValue(UnsafeSanitizedContentOrdainer.ordainAsSafe(
            "foo() + bar", SanitizedContent.ContentKind.HTML)));
  }

  public final void testEscapeCssString() {
    // The minimal escapes.
    // Do not remove anything from this set without talking to your friendly local security-team@.
    assertEquals(
        "\\0  \\22  \\27  \\5c  \\a  \\c  \\d ",
        Sanitizers.escapeCssString("\u0000 \" \' \\ \n \u000c \r"));

    for (String hazard : EMBEDDING_HAZARDS) {
      assertFalse(hazard, Sanitizers.escapeCssString(hazard).contains(hazard));
    }

    String escapedAscii = (
        "\\0 \u0001\u0002\u0003\u0004\u0005\u0006\u0007\\8 \\9 \\a \\b \\c \\d \u000e\u000f" +
        "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017" +
        "\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F" +
        " !\\22 #$%\\26 \\27 \\28 \\29 \\2a +,-.\\2f " +
        "0123456789\\3a \\3b \\3c \\3d \\3e ?" +
        "\\40 ABCDEFGHIJKLMNO" +
        "PQRSTUVWXYZ[\\5c ]^_" +
        "`abcdefghijklmno" +
        "pqrstuvwxyz\\7b |\\7d ~\u007f");
    assertEquals(escapedAscii, Sanitizers.escapeCssString(ASCII_CHARS));
    assertEquals(escapedAscii, Sanitizers.escapeCssString(ASCII_CHARS_SOYDATA));
  }

  public final void testFilterCssValue() {
    assertEquals("33px", Sanitizers.filterCssValue("33px"));
    assertEquals("33px", Sanitizers.filterCssValue(SoyData.createFromExistingData("33px")));
    assertEquals("-.5em", Sanitizers.filterCssValue("-.5em"));
    assertEquals("inherit", Sanitizers.filterCssValue("inherit"));
    assertEquals("display", Sanitizers.filterCssValue("display"));
    assertEquals("none", Sanitizers.filterCssValue("none"));
    assertEquals("#id", Sanitizers.filterCssValue("#id"));
    assertEquals(".class", Sanitizers.filterCssValue(".class"));
    assertEquals("red", Sanitizers.filterCssValue("red"));
    assertEquals("#aabbcc", Sanitizers.filterCssValue("#aabbcc"));
    assertEquals("zSoyz", Sanitizers.filterCssValue("expression"));
    assertEquals("zSoyz", Sanitizers.filterCssValue(SoyData.createFromExistingData("expression")));
    assertEquals("zSoyz", Sanitizers.filterCssValue("Expression"));
    assertEquals("zSoyz", Sanitizers.filterCssValue("\\65xpression"));
    assertEquals("zSoyz", Sanitizers.filterCssValue("\\65 xpression"));
    assertEquals("zSoyz", Sanitizers.filterCssValue("-moz-binding"));
    assertEquals("zSoyz", Sanitizers.filterCssValue("</style><script>alert('foo')</script>/*"));
    assertEquals("zSoyz", Sanitizers.filterCssValue("color:expression('whatever')"));
    assertEquals("color:expression('whatever')", Sanitizers.filterCssValue(
        UnsafeSanitizedContentOrdainer.ordainAsSafe(
            "color:expression('whatever')", SanitizedContent.ContentKind.CSS)));

    for (String hazard : EMBEDDING_HAZARDS) {
      assertFalse(hazard, Sanitizers.filterCssValue(hazard).contains(hazard));
    }
  }

  public final void testFilterHtmlAttributes() {
    assertEquals("dir", Sanitizers.filterHtmlAttributes("dir"));
    assertEquals("dir", Sanitizers.filterHtmlAttributes(SoyData.createFromExistingData("dir")));
    assertEquals("zSoyz", Sanitizers.filterHtmlAttributes("><script>alert('foo')</script"));
    assertEquals("zSoyz", Sanitizers.filterHtmlAttributes("style"));
    assertEquals("zSoyz", Sanitizers.filterHtmlAttributes("onclick"));
    assertEquals("zSoyz", Sanitizers.filterHtmlAttributes("href"));

    assertEquals(
        "a=1 b=2 dir=\"ltr\"",
        Sanitizers.filterHtmlAttributes(SoyData.createFromExistingData(
            UnsafeSanitizedContentOrdainer.ordainAsSafe(
                "a=1 b=2 dir=\"ltr\"", SanitizedContent.ContentKind.ATTRIBUTES))));
    assertEquals(
        "Should append a space to parse correctly",
        "foo=\"bar\" dir=ltr ",
        Sanitizers.filterHtmlAttributes(SoyData.createFromExistingData(
            UnsafeSanitizedContentOrdainer.ordainAsSafe(
                "foo=\"bar\" dir=ltr", SanitizedContent.ContentKind.ATTRIBUTES))));
    assertEquals(
        "Should append a space to parse correctly",
        "foo=\"bar\" checked ",
        Sanitizers.filterHtmlAttributes(SoyData.createFromExistingData(
            UnsafeSanitizedContentOrdainer.ordainAsSafe(
                "foo=\"bar\" checked", SanitizedContent.ContentKind.ATTRIBUTES))));
    assertEquals(
        "No duplicate space should be added",
        "foo=\"bar\" checked ",
        Sanitizers.filterHtmlAttributes(SoyData.createFromExistingData(
            UnsafeSanitizedContentOrdainer.ordainAsSafe(
                "foo=\"bar\" checked ", SanitizedContent.ContentKind.ATTRIBUTES))));

    for (String hazard : EMBEDDING_HAZARDS) {
      assertFalse(hazard, Sanitizers.filterHtmlAttributes(hazard).contains(hazard));
    }
  }

  public final void testFilterHtmlElementName() {
    assertEquals("h1", Sanitizers.filterHtmlElementName("h1"));
    assertEquals("h1", Sanitizers.filterHtmlElementName(SoyData.createFromExistingData("h1")));
    assertEquals("zSoyz", Sanitizers.filterHtmlElementName("script"));
    assertEquals("zSoyz", Sanitizers.filterHtmlElementName("style"));
    assertEquals("zSoyz", Sanitizers.filterHtmlElementName("SCRIPT"));
    assertEquals("zSoyz", Sanitizers.filterHtmlElementName("><script>alert('foo')</script"));
    assertEquals("zSoyz", Sanitizers.filterHtmlElementName(SoyData.createFromExistingData("<h1>")));

    for (String hazard : EMBEDDING_HAZARDS) {
      assertFalse(hazard, Sanitizers.filterHtmlElementName(hazard).contains(hazard));
    }
  }

  public final void testEscapeUri() {
    // The minimal escapes.
    // Do not remove anything from this set without talking to your friendly local security-team@.
    assertEquals(
        "%00%0A%0C%0D%22%23%26%27%2F%3A%3D%3F%40",
        Sanitizers.escapeUri("\u0000\n\f\r\"#&'/:=?@"));

    for (String hazard : EMBEDDING_HAZARDS) {
      assertFalse(hazard, Sanitizers.escapeUri(hazard).contains(hazard));
    }

    String escapedAscii = (
        "%00%01%02%03%04%05%06%07%08%09%0A%0B%0C%0D%0E%0F" +
        "%10%11%12%13%14%15%16%17%18%19%1A%1B%1C%1D%1E%1F" +
        "%20%21%22%23%24%25%26%27%28%29*%2B%2C-.%2F" +
        "0123456789%3A%3B%3C%3D%3E%3F" +
        "%40ABCDEFGHIJKLMNO" +
        "PQRSTUVWXYZ%5B%5C%5D%5E_" +
        "%60abcdefghijklmno" +
        "pqrstuvwxyz%7B%7C%7D%7E%7F");
    assertEquals(escapedAscii, Sanitizers.escapeUri(ASCII_CHARS));
    assertEquals(escapedAscii, Sanitizers.escapeUri(ASCII_CHARS_SOYDATA));
    // Test full-width.  The two characters at the right are a full-width '#' and ':'.
    assertEquals("%EF%BC%83%EF%BC%9A", Sanitizers.escapeUri("\uff03\uff1a"));
    // Test other unicode codepoints.
    assertEquals("%C2%85%E2%80%A8", Sanitizers.escapeUri("\u0085\u2028"));
    // Test Sanitized Content of the right kind. Note that some characters are still normalized --
    // specifically, ones that are allowed in URI's but not attributes but don't change meaning
    // (and parentheses since they're technically reserved).
    assertEquals("foo%28%27&%27%29", Sanitizers.escapeUri(
        UnsafeSanitizedContentOrdainer.ordainAsSafe(
            "foo(%27&')", SanitizedContent.ContentKind.URI)));
    // Test SanitizedContent of the wrong kind -- it should be completely escaped.
    assertEquals("%2528%2529", Sanitizers.escapeUri(
        UnsafeSanitizedContentOrdainer.ordainAsSafe(
            "%28%29", SanitizedContent.ContentKind.HTML)));
  }

  public final void testNormalizeUriAndFilterNormalizeUri() {
    for (String hazard : EMBEDDING_HAZARDS) {
      assertFalse(hazard, Sanitizers.normalizeUri(hazard).contains(hazard));
      assertFalse(hazard, Sanitizers.filterNormalizeUri(hazard).contains(hazard));
    }

    String escapedAscii = (
        "%00%01%02%03%04%05%06%07%08%09%0A%0B%0C%0D%0E%0F" +
        "%10%11%12%13%14%15%16%17%18%19%1A%1B%1C%1D%1E%1F" +
        "%20!%22#$%&%27%28%29*+,-./" +
        "0123456789:;%3C=%3E?" +
        "@ABCDEFGHIJKLMNO" +
        "PQRSTUVWXYZ[%5C]^_" +
        "`abcdefghijklmno" +
        "pqrstuvwxyz%7B|%7D~%7F");
    assertEquals(escapedAscii, Sanitizers.normalizeUri(ASCII_CHARS));
    assertEquals(escapedAscii, Sanitizers.normalizeUri(ASCII_CHARS_SOYDATA));
    assertEquals("#" + escapedAscii, Sanitizers.filterNormalizeUri("#" + ASCII_CHARS));

    // Test full-width.  The two characters at the right are a full-width '#' and ':'.
    String escapedFullWidth = "%EF%BC%83%EF%BC%9A";
    String fullWidth = "\uff03\uff1a";
    assertEquals(escapedFullWidth, Sanitizers.normalizeUri(fullWidth));
    assertEquals(escapedFullWidth, Sanitizers.filterNormalizeUri(fullWidth));

    // Test filtering of URI starts.
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("javascript:"));
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("javascript:alert(1337)"));
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("vbscript:alert(1337)"));
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("livescript:alert(1337)"));
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("data:,alert(1337)"));
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("data:text/javascript,alert%281337%29"));
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("file:///etc/passwd"));
    assertFalse(
        Sanitizers.filterNormalizeUri("javascript\uff1aalert(1337);")
        .contains("javascript\uff1a"));

    // Testcases from http://ha.ckers.org/xss.html
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("JaVaScRiPt:alert(1337)"));
    assertEquals(
        "#zSoyz",
        Sanitizers.filterNormalizeUri(
            // Using HTML entities to obfuscate javascript:alert('XSS');
            "&#106;&#97;&#118;&#97;&#115;&#99;&#114;&#105;&#112;&#116;" +
            "&#58;&#97;&#108;&#101;&#114;&#116;&#40;&#39;&#88;&#83;&#83;&#39;&#41;"));
    assertEquals(
        "#zSoyz",
        Sanitizers.filterNormalizeUri(  // Using longer HTML entities to obfuscate the same.
            "&#0000106&#0000097&#0000118&#0000097&#0000115&#0000099&#0000114&#0000105" +
            "&#0000112&#0000116&#0000058&#0000097&#0000108&#0000101&#0000114&#0000116" +
            "&#0000040&#0000039&#0000088&#0000083&#0000083&#0000039&#0000041"));
    assertEquals(
        "#zSoyz",
        Sanitizers.filterNormalizeUri(  // Using hex HTML entities to obfuscate the same.
            "&#x6A&#x61&#x76&#x61&#x73&#x63&#x72&#x69&#x70&#x74" +
            "&#x3A&#x61&#x6C&#x65&#x72&#x74&#x28&#x27&#x58&#x53&#x53&#x27&#x29"));
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("jav\tascript:alert('XSS');"));
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("jav&#x09;ascript:alert('XSS');"));
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("jav&#x0A;ascript:alert('XSS');"));
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("jav&#x0D;ascript:alert('XSS');"));
    assertEquals(
        "#zSoyz",
        Sanitizers.filterNormalizeUri(
            "\nj\n\na\nv\na\ns\nc\nr\ni\np\nt\n:\na\nl\ne\nr\nt\n(\n1\n3\n3\n7\n)"));
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri("\u000e  javascript:alert('XSS');"));

    // Things we should accept.
    assertEquals("http://google.com/", Sanitizers.filterNormalizeUri("http://google.com/"));
    assertEquals("https://google.com/", Sanitizers.filterNormalizeUri("https://google.com/"));
    assertEquals("HTTP://google.com/", Sanitizers.filterNormalizeUri("HTTP://google.com/"));
    assertEquals("?a=b&c=d", Sanitizers.filterNormalizeUri("?a=b&c=d"));
    assertEquals("?a=b:c&d=e", Sanitizers.filterNormalizeUri("?a=b:c&d=e"));
    assertEquals("//foo.com:80/", Sanitizers.filterNormalizeUri("//foo.com:80/"));
    assertEquals("//foo.com/", Sanitizers.filterNormalizeUri("//foo.com/"));
    assertEquals("/foo:bar/", Sanitizers.filterNormalizeUri("/foo:bar/"));
    assertEquals("#a:b", Sanitizers.filterNormalizeUri("#a:b"));
    assertEquals("#", Sanitizers.filterNormalizeUri("#"));
    assertEquals("/", Sanitizers.filterNormalizeUri("/"));
    assertEquals("", Sanitizers.filterNormalizeUri(""));
    assertEquals("javascript:handleClick%28%29", Sanitizers.filterNormalizeUri(
        UnsafeSanitizedContentOrdainer.ordainAsSafe(
            "javascript:handleClick()", SanitizedContent.ContentKind.URI)));
    assertEquals("#zSoyz", Sanitizers.filterNormalizeUri(
        UnsafeSanitizedContentOrdainer.ordainAsSafe(
            "javascript:handleClick()", SanitizedContent.ContentKind.HTML)));
  }

  public final void testEscapeHtml() {
    String escapedAscii = (
        "&#0;\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000b\f\r\u000e\u000f" +
        "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017" +
        "\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f" +
        " !&quot;#$%&amp;&#39;()*+,-./" +
        "0123456789:;&lt;=&gt;?" +
        "@ABCDEFGHIJKLMNO" +
        "PQRSTUVWXYZ[\\]^_" +
        "`abcdefghijklmno" +
        "pqrstuvwxyz{|}~\u007f");
    assertEquals(escapedAscii, Sanitizers.escapeHtml(ASCII_CHARS));
    assertEquals(escapedAscii, Sanitizers.escapeHtml(ASCII_CHARS_SOYDATA));
  }

  public final void testEscapeHtmlAttributeNospace() {
    // The minimal escapes.
    // Do not remove anything from this set without talking to your friendly local security-team@.
    assertEquals(
        "&#9;&#10;&#11;&#12;&#13;&#32;&quot;&#39;&#96;&lt;&gt;&amp;",
        Sanitizers.escapeHtmlAttributeNospace("\u0009\n\u000B\u000C\r \"'\u0060<>&"));

    String escapedAscii = (
        "&#0;\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b&#9;&#10;&#11;&#12;&#13;\u000e\u000f" +
        "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017" +
        "\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f" +
        "&#32;!&quot;#$%&amp;&#39;()*+,&#45;.&#47;" +
        "0123456789:;&lt;&#61;&gt;?" +
        "@ABCDEFGHIJKLMNO" +
        "PQRSTUVWXYZ[\\]^_" +
        "&#96;abcdefghijklmno" +
        "pqrstuvwxyz{|}~\u007f");
    assertEquals(escapedAscii, Sanitizers.escapeHtmlAttributeNospace(ASCII_CHARS));
    assertEquals(escapedAscii, Sanitizers.escapeHtmlAttributeNospace(ASCII_CHARS_SOYDATA));
  }

  public final void testNormalizeHtml() {
    // The minimal escapes.
    // Do not remove anything from this set without talking to your friendly local security-team@.
    assertEquals(
        "&quot;&#39;&lt;&gt;",
        Sanitizers.normalizeHtml("\"'<>"));

    String escapedAscii = (
        "&#0;\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000b\u000c\r\u000e\u000f" +
        "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017" +
        "\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f" +
        " !&quot;#$%&&#39;()*+,-./" +
        "0123456789:;&lt;=&gt;?" +
        "@ABCDEFGHIJKLMNO" +
        "PQRSTUVWXYZ[\\]^_" +
        "`abcdefghijklmno" +
        "pqrstuvwxyz{|}~\u007f");
    assertEquals(escapedAscii, Sanitizers.normalizeHtml(ASCII_CHARS));
    assertEquals(escapedAscii, Sanitizers.normalizeHtml(ASCII_CHARS_SOYDATA));
  }

  public final void testNormalizeHtmlNospace() {
    // The minimal escapes.
    // Do not remove anything from this set without talking to your friendly local security-team@.
    assertEquals(
        "&#9;&#10;&#11;&#12;&#13;&#32;&quot;&#39;&#96;&lt;&gt;",
        Sanitizers.normalizeHtmlNospace("\u0009\n\u000B\u000C\r \"'\u0060<>"));

    String escapedAscii = (
        "&#0;\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b&#9;&#10;&#11;&#12;&#13;\u000e\u000f" +
        "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017" +
        "\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f" +
        "&#32;!&quot;#$%&&#39;()*+,&#45;.&#47;" +
        "0123456789:;&lt;&#61;&gt;?" +
        "@ABCDEFGHIJKLMNO" +
        "PQRSTUVWXYZ[\\]^_" +
        "&#96;abcdefghijklmno" +
        "pqrstuvwxyz{|}~\u007f");
    assertEquals(escapedAscii, Sanitizers.normalizeHtmlNospace(ASCII_CHARS));
    assertEquals(escapedAscii, Sanitizers.normalizeHtmlNospace(ASCII_CHARS_SOYDATA));
  }

  private static final String stripHtmlTags(String html, boolean spacesOk) {
    return Sanitizers.stripHtmlTags(html, null, spacesOk);
  }

  public final void testStripHtmlTags() {
    assertEquals("", stripHtmlTags("", true));
    assertEquals("Hello, World!", stripHtmlTags("Hello, World!", true));
    assertEquals("Hello,&#32;World!", stripHtmlTags("Hello, World!", false));
    assertEquals("Hello, World!", stripHtmlTags("<b>Hello, World!</b>", true));
    assertEquals(
        "Hello, &quot;World!&quot;", stripHtmlTags("<b>Hello, \"World!\"</b>", true));
    assertEquals(
        "Hello,&#32;&quot;World!&quot;",
        stripHtmlTags("<b>Hello, \"World!\"</b>", false));
    assertEquals("42", stripHtmlTags("42", true));
    // Don't merge content around tags into an entity.
    assertEquals("&amp;amp;", stripHtmlTags("&<hr>amp;", true));
  }

  private static final TagWhitelist TEST_WHITELIST = new TagWhitelist(
      "b", "br", "ul", "li", "table", "tr", "td");

  private static final String cleanHtml(String html) {
    return Sanitizers.stripHtmlTags(html, TEST_WHITELIST, true);
  }

  public final void testTagWhitelisting() {
    assertEquals("<b>Hello, World!</b>", cleanHtml("<b>Hello, World!</b>"));
    assertEquals("<b>Hello, World!</b>", cleanHtml("<b onclick='evil()'>Hello, World!</b>"));
    assertEquals("<b>Hello, <br> World!</b>", cleanHtml("<b>Hello, <br/> World!</b>"));
    // Don't add end tags for void elements.
    assertEquals("<b>Hello, <br> World!</b>", cleanHtml("<b>Hello, <br/> World!"));
    assertEquals("<b>Hello, <br> World!</b>", cleanHtml("<b>Hello, <br> World!"));
    // Missing open tag.
    assertEquals("Hello, <br> World!", cleanHtml("Hello, <br/> World!"));
    // A truncated tag is not a tag.
    assertEquals("Hello, &lt;br", cleanHtml("Hello, <br"));
    // Test boundary conditions at end of input.
    assertEquals("Hello, &lt;", cleanHtml("Hello, <"));
    assertEquals("Hello, &lt;/", cleanHtml("Hello, </"));
    assertEquals("Hello, &lt; World", cleanHtml("Hello, < World"));
    // Don't be confused by attributes that merge into the tag name.
    assertEquals("", cleanHtml("<img/onload=alert(1337)>"));
    assertEquals("foo", cleanHtml("<i/onmouseover=alert(1337)>foo</i>"));
    assertEquals("AB", cleanHtml("A<img/onload=alert(1337)>B"));
    // Don't create new tags from parts that were not originally adjacent.
    assertEquals(
        "&lt;img onload=alert(1337)",
        cleanHtml("<<img/onload=alert(1337)>img onload=alert(1337)"));
    // Test external layout breakers.
    // <ul><li>Foo</ul></li> would be bad since it is equivalent to
    // <ul><li>Foo</li></ul></li>
    assertEquals("<ul><li>Foo</li></ul>", cleanHtml("<ul><li>Foo</ul>"));
    // We put the close tags in the wrong place but in a way that is safe.
    assertEquals("<ul><li>1<li>2</li></li></ul>", cleanHtml("<ul><li>1<li>2</ul>"));
    assertEquals("<table><tr><td></td></tr></table>", cleanHtml("<table><tr><td>"));
    // Don't merge content around tags into an entity.
    assertEquals("&amp;amp;", cleanHtml("&<hr>amp;"));
  }

  public final void testFilterNoAutoescape() {
    // Filter out anything marked with sanitized content of kind "text" which indicates it
    // previously was constructed without any escaping.
    assertEquals(SoyData.createFromExistingData("zSoyz"), Sanitizers.filterNoAutoescape(
        UnsafeSanitizedContentOrdainer.ordainAsSafe("x", SanitizedContent.ContentKind.TEXT)));
    assertEquals(SoyData.createFromExistingData("zSoyz"),
        Sanitizers.filterNoAutoescape(UnsafeSanitizedContentOrdainer.ordainAsSafe(
            "<!@*!@(*!@(>", SanitizedContent.ContentKind.TEXT)));

    // Everything else should be let through. Hope it's safe!
    assertEquals(SoyData.createFromExistingData("<div>test</div>"),
        Sanitizers.filterNoAutoescape(UnsafeSanitizedContentOrdainer.ordainAsSafe(
            "<div>test</div>", SanitizedContent.ContentKind.HTML)));
    assertEquals(SoyData.createFromExistingData("foo='bar'"),
        Sanitizers.filterNoAutoescape(UnsafeSanitizedContentOrdainer.ordainAsSafe(
            "foo='bar'", SanitizedContent.ContentKind.ATTRIBUTES)));
    assertEquals(SoyData.createFromExistingData(".foo{color:green}"),
        Sanitizers.filterNoAutoescape(UnsafeSanitizedContentOrdainer.ordainAsSafe(
            ".foo{color:green}", SanitizedContent.ContentKind.CSS)));
    assertEquals(SoyData.createFromExistingData("<div>test</div>"),
        Sanitizers.filterNoAutoescape(SoyData.createFromExistingData("<div>test</div>")));
    assertEquals(SoyData.createFromExistingData("null"),
        Sanitizers.filterNoAutoescape(SoyData.createFromExistingData(null)));
    assertEquals(SoyData.createFromExistingData("123"),
        Sanitizers.filterNoAutoescape(SoyData.createFromExistingData(123)));
  }
}
