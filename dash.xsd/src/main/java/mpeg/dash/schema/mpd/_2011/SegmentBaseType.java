//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0-b170531.0717 generiert 
// Siehe <a href="https://jaxb.java.net/">https://jaxb.java.net/</a> 
// xc4nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2018.03.28 um 10:20:11 AM CEST 
//


package mpeg.dash.schema.mpd._2011;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import org.w3c.dom.Element;


/**
 * <p>Java-Klasse fxFCr SegmentBaseType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="SegmentBaseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Initialization" type="{urn:mpeg:dash:schema:mpd:2011}URLType" minOccurs="0"/&gt;
 *         &lt;element name="RepresentationIndex" type="{urn:mpeg:dash:schema:mpd:2011}URLType" minOccurs="0"/&gt;
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="timescale" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="presentationTimeOffset" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" /&gt;
 *       &lt;attribute name="indexRange" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="indexRangeExact" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt;
 *       &lt;attribute name="availabilityTimeOffset" type="{http://www.w3.org/2001/XMLSchema}double" /&gt;
 *       &lt;attribute name="availabilityTimeComplete" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SegmentBaseType", propOrder = {
    "initElement",
    "representationIndex",
    "any"
})
@XmlSeeAlso({
    MultipleSegmentBaseType.class
})
public class SegmentBaseType {

    @XmlElement(name = "Initialization")
    protected URLType initElement;
    @XmlElement(name = "RepresentationIndex")
    protected URLType representationIndex;
    @XmlAnyElement(lax = true)
    protected List<Object> any;
    @XmlAttribute(name = "timescale")
    @XmlSchemaType(name = "unsignedInt")
    protected Long timescale;
    @XmlAttribute(name = "presentationTimeOffset")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger presentationTimeOffset;
    @XmlAttribute(name = "indexRange")
    protected String indexRange;
    @XmlAttribute(name = "indexRangeExact")
    protected Boolean indexRangeExact;
    @XmlAttribute(name = "availabilityTimeOffset")
    protected Double availabilityTimeOffset;
    @XmlAttribute(name = "availabilityTimeComplete")
    protected Boolean availabilityTimeComplete;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Ruft den Wert der initElement-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link URLType }
     *     
     */
    public URLType getInitElement() {
        return initElement;
    }

    /**
     * Legt den Wert der initElement-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link URLType }
     *     
     */
    public void setInitElement(URLType value) {
        this.initElement = value;
    }

    /**
     * Ruft den Wert der representationIndex-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link URLType }
     *     
     */
    public URLType getRepresentationIndex() {
        return representationIndex;
    }

    /**
     * Legt den Wert der representationIndex-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link URLType }
     *     
     */
    public void setRepresentationIndex(URLType value) {
        this.representationIndex = value;
    }

    /**
     * Gets the value of the any property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the any property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAny().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Element }
     * {@link Object }
     * 
     * 
     */
    public List<Object> getAny() {
        if (any == null) {
            any = new ArrayList<Object>();
        }
        return this.any;
    }

    /**
     * Ruft den Wert der timescale-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getTimescale() {
        return timescale;
    }

    /**
     * Legt den Wert der timescale-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setTimescale(Long value) {
        this.timescale = value;
    }

    /**
     * Ruft den Wert der presentationTimeOffset-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getPresentationTimeOffset() {
        return presentationTimeOffset;
    }

    /**
     * Legt den Wert der presentationTimeOffset-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setPresentationTimeOffset(BigInteger value) {
        this.presentationTimeOffset = value;
    }

    /**
     * Ruft den Wert der indexRange-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIndexRange() {
        return indexRange;
    }

    /**
     * Legt den Wert der indexRange-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIndexRange(String value) {
        this.indexRange = value;
    }

    /**
     * Ruft den Wert der indexRangeExact-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isIndexRangeExact() {
        if (indexRangeExact == null) {
            return false;
        } else {
            return indexRangeExact;
        }
    }

    /**
     * Legt den Wert der indexRangeExact-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIndexRangeExact(Boolean value) {
        this.indexRangeExact = value;
    }

    /**
     * Ruft den Wert der availabilityTimeOffset-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getAvailabilityTimeOffset() {
        return availabilityTimeOffset;
    }

    /**
     * Legt den Wert der availabilityTimeOffset-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setAvailabilityTimeOffset(Double value) {
        this.availabilityTimeOffset = value;
    }

    /**
     * Ruft den Wert der availabilityTimeComplete-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAvailabilityTimeComplete() {
        return availabilityTimeComplete;
    }

    /**
     * Legt den Wert der availabilityTimeComplete-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAvailabilityTimeComplete(Boolean value) {
        this.availabilityTimeComplete = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     * 
     * <p>
     * the map is keyed by the name of the attribute and 
     * the value is the string value of the attribute.
     * 
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     * 
     * 
     * @return
     *     always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
