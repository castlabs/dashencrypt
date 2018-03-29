//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0-b170531.0717 generiert 
// Siehe <a href="https://jaxb.java.net/">https://jaxb.java.net/</a> 
// xc4nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2018.03.28 um 10:20:11 AM CEST 
//


package mpeg.dash.schema.mpd._2011;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse fxFCr VideoScanType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="VideoScanType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="progressive"/&gt;
 *     &lt;enumeration value="interlaced"/&gt;
 *     &lt;enumeration value="unknown"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "VideoScanType")
@XmlEnum
public enum VideoScanType {

    @XmlEnumValue("progressive")
    PROGRESSIVE("progressive"),
    @XmlEnumValue("interlaced")
    INTERLACED("interlaced"),
    @XmlEnumValue("unknown")
    UNKNOWN("unknown");
    private final String value;

    VideoScanType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static VideoScanType fromValue(String v) {
        for (VideoScanType c: VideoScanType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
