//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0-b170531.0717 generiert 
// Siehe <a href="https://jaxb.java.net/">https://jaxb.java.net/</a> 
// xc4nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2018.03.28 um 10:20:11 AM CEST 
//


package mpeg.dash.schema.mpd._2011;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse fxFCr MultipleSegmentBaseType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="MultipleSegmentBaseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:mpeg:dash:schema:mpd:2011}SegmentBaseType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SegmentTimeline" type="{urn:mpeg:dash:schema:mpd:2011}SegmentTimelineType" minOccurs="0"/&gt;
 *         &lt;element name="BitstreamSwitching" type="{urn:mpeg:dash:schema:mpd:2011}URLType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="duration" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="startNumber" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MultipleSegmentBaseType", propOrder = {
    "segmentTimeline",
    "bitstreamSwitchingElement"
})
@XmlSeeAlso({
    SegmentListType.class,
    SegmentTemplateType.class
})
public class MultipleSegmentBaseType
    extends SegmentBaseType
{

    @XmlElement(name = "SegmentTimeline")
    protected SegmentTimelineType segmentTimeline;
    @XmlElement(name = "BitstreamSwitching")
    protected URLType bitstreamSwitchingElement;
    @XmlAttribute(name = "duration")
    @XmlSchemaType(name = "unsignedInt")
    protected Long duration;
    @XmlAttribute(name = "startNumber")
    @XmlSchemaType(name = "unsignedInt")
    protected Long startNumber;

    /**
     * Ruft den Wert der segmentTimeline-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link SegmentTimelineType }
     *     
     */
    public SegmentTimelineType getSegmentTimeline() {
        return segmentTimeline;
    }

    /**
     * Legt den Wert der segmentTimeline-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link SegmentTimelineType }
     *     
     */
    public void setSegmentTimeline(SegmentTimelineType value) {
        this.segmentTimeline = value;
    }

    /**
     * Ruft den Wert der bitstreamSwitchingElement-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link URLType }
     *     
     */
    public URLType getBitstreamSwitchingElement() {
        return bitstreamSwitchingElement;
    }

    /**
     * Legt den Wert der bitstreamSwitchingElement-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link URLType }
     *     
     */
    public void setBitstreamSwitchingElement(URLType value) {
        this.bitstreamSwitchingElement = value;
    }

    /**
     * Ruft den Wert der duration-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getDuration() {
        return duration;
    }

    /**
     * Legt den Wert der duration-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setDuration(Long value) {
        this.duration = value;
    }

    /**
     * Ruft den Wert der startNumber-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getStartNumber() {
        return startNumber;
    }

    /**
     * Legt den Wert der startNumber-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setStartNumber(Long value) {
        this.startNumber = value;
    }

}
