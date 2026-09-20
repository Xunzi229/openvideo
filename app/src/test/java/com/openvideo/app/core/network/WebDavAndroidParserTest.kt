package com.openvideo.app.core.network

import org.junit.Assert.*
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [28], application = android.app.Application::class)
class WebDavAndroidParserTest {
    private fun androidFactory() = Class.forName("org.apache.harmony.xml.parsers.DocumentBuilderFactoryImpl")
        .getDeclaredConstructor().newInstance() as DocumentBuilderFactory

    @Test fun validDirectoryParsesUsingAndroidFactory() {
        val result = WebDavDirectoryParser.parse("https://host/dav/", """
            <d:multistatus xmlns:d="DAV:"><d:response><d:href>/dav/a.mp4</d:href>
            <d:propstat><d:prop><d:displayname>A &amp; B</d:displayname></d:prop></d:propstat>
            </d:response></d:multistatus>
        """.trimIndent(), androidFactory())
        assertEquals("A & B", result.single().name)
    }

    @Test fun internalAndExternalDtdsAreRejectedBeforeEitherFactoryParses() {
        for (factory in listOf(androidFactory(), DocumentBuilderFactory.newInstance())) {
            for (declaration in listOf("<!DOCTYPE x SYSTEM 'file:///private'>", "<!DOCTYPE x [<!ENTITY a 'expanded'>]>")) {
                assertThrows(IllegalArgumentException::class.java) {
                    WebDavDirectoryParser.parse("https://host/dav/", "$declaration<x/>", factory)
                }
            }
        }
    }
}
