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
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse fxFCr SegmentTemplateType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="SegmentTemplateType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:mpeg:dash:schema:mpd:2011}MultipleSegmentBaseType"&gt;
 *       &lt;attribute name="media" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="index" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="initialization" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="bitstreamSwitching" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SegmentTemplateType")
public class SegmentTemplateType
    extends MultipleSegmentBaseType
{

    @XmlAttribute(name = "media")
    protected String media;
    @XmlAttribute(name = "index")
    protected String index;
    @XmlAttribute(name = "initialization")
    protected String initializationAttribute;
    @XmlAttribute(name = "bitstreamSwitching")
    protected String bitstreamSwitchingAttr;

    /**
     * Ruft den Wert der media-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMedia() {
        return media;
    }

    /**
     * Legt den Wert der media-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMedia(String value) {
        this.media = value;
    }

    /**
     * Ruft den Wert der index-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIndex() {
        return index;
    }

    /**
     * Legt den Wert der index-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIndex(String value) {
        this.index = value;
    }

    /**
     * Ruft den Wert der initializationAttribute-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInitializationAttribute() {
        return initializationAttribute;
    }

    /**
     * Legt den Wert der initializationAttribute-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInitializationAttribute(String value) {
        this.initializationAttribute = value;
    }

    /**
     * Ruft den Wert der bitstreamSwitchingAttr-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBitstreamSwitchingAttr() {
        return bitstreamSwitchingAttr;
    }

    /**
     * Legt den Wert der bitstreamSwitchingAttr-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBitstreamSwitchingAttr(String value) {
        this.bitstreamSwitchingAttr = value;
    }

}
