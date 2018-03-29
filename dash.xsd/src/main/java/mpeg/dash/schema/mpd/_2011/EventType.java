//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0-b170531.0717 generiert 
// Siehe <a href="https://jaxb.java.net/">https://jaxb.java.net/</a> 
// xc4nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2018.03.28 um 10:20:11 AM CEST 
//


package mpeg.dash.schema.mpd._2011;

import java.math.BigInteger;
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
 * <p>Java-Klasse fxFCr EventType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="EventType"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *       &lt;attribute name="presentationTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" default="0" /&gt;
 *       &lt;attribute name="duration" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" /&gt;
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventType", propOrder = {
    "value"
})
public class EventType {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "presentationTime")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger presentationTime;
    @XmlAttribute(name = "duration")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger duration;
    @XmlAttribute(name = "id")
    @XmlSchemaType(name = "unsignedInt")
    protected Long id;
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
     * Ruft den Wert der presentationTime-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getPresentationTime() {
        if (presentationTime == null) {
            return new BigInteger("0");
        } else {
            return presentationTime;
        }
    }

    /**
     * Legt den Wert der presentationTime-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setPresentationTime(BigInteger value) {
        this.presentationTime = value;
    }

    /**
     * Ruft den Wert der duration-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getDuration() {
        return duration;
    }

    /**
     * Legt den Wert der duration-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setDuration(BigInteger value) {
        this.duration = value;
    }

    /**
     * Ruft den Wert der id-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getId() {
        return id;
    }

    /**
     * Legt den Wert der id-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setId(Long value) {
        this.id = value;
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
