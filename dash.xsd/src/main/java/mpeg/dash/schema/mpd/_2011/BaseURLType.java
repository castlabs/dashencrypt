//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0-b170531.0717 generiert 
// Siehe <a href="https://jaxb.java.net/">https://jaxb.java.net/</a> 
// xc4nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2018.03.28 um 10:20:11 AM CEST 
//


package mpeg.dash.schema.mpd._2011;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;


/**
 * <p>Java-Klasse fxFCr BaseURLType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="BaseURLType"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;anyURI"&gt;
 *       &lt;attribute name="serviceLocation" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="byteRange" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="availabilityTimeOffset" type="{http://www.w3.org/2001/XMLSchema}double" /&gt;
 *       &lt;attribute name="availabilityTimeComplete" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BaseURLType", propOrder = {
    "value"
})
public class BaseURLType {

    @XmlValue
    @XmlSchemaType(name = "anyURI")
    protected String value;
    @XmlAttribute(name = "serviceLocation")
    protected String serviceLocation;
    @XmlAttribute(name = "byteRange")
    protected String byteRange;
    @XmlAttribute(name = "availabilityTimeOffset")
    protected Double availabilityTimeOffset;
    @XmlAttribute(name = "availabilityTimeComplete")
    protected Boolean availabilityTimeComplete;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Ruft den Wert der value-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Legt den Wert der value-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Ruft den Wert der serviceLocation-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getServiceLocation() {
        return serviceLocation;
    }

    /**
     * Legt den Wert der serviceLocation-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setServiceLocation(String value) {
        this.serviceLocation = value;
    }

    /**
     * Ruft den Wert der byteRange-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getByteRange() {
        return byteRange;
    }

    /**
     * Legt den Wert der byteRange-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setByteRange(String value) {
        this.byteRange = value;
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
