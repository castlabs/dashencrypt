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
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import org.w3c.dom.Element;


/**
 * <p>Java-Klasse fxFCr SegmentTimelineType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="SegmentTimelineType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="S" maxOccurs="unbounded"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;attribute name="t" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" /&gt;
 *                 &lt;attribute name="d" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" /&gt;
 *                 &lt;attribute name="r" type="{http://www.w3.org/2001/XMLSchema}integer" default="0" /&gt;
 *                 &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SegmentTimelineType", propOrder = {
    "s",
    "any"
})
public class SegmentTimelineType {

    @XmlElement(name = "S", required = true)
    protected List<SegmentTimelineType.S> s;
    @XmlAnyElement(lax = true)
    protected List<Object> any;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the s property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the s property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getS().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SegmentTimelineType.S }
     * 
     * 
     */
    public List<SegmentTimelineType.S> getS() {
        if (s == null) {
            s = new ArrayList<SegmentTimelineType.S>();
        }
        return this.s;
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


    /**
     * <p>Java-Klasse fxFCr anonymous complex type.
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;attribute name="t" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" /&gt;
     *       &lt;attribute name="d" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" /&gt;
     *       &lt;attribute name="r" type="{http://www.w3.org/2001/XMLSchema}integer" default="0" /&gt;
     *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class S {

        @XmlAttribute(name = "t")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger t;
        @XmlAttribute(name = "d", required = true)
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger d;
        @XmlAttribute(name = "r")
        protected BigInteger r;
        @XmlAnyAttribute
        private Map<QName, String> otherAttributes = new HashMap<QName, String>();

        /**
         * Ruft den Wert der t-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getT() {
            return t;
        }

        /**
         * Legt den Wert der t-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setT(BigInteger value) {
            this.t = value;
        }

        /**
         * Ruft den Wert der d-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getD() {
            return d;
        }

        /**
         * Legt den Wert der d-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setD(BigInteger value) {
            this.d = value;
        }

        /**
         * Ruft den Wert der r-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getR() {
            if (r == null) {
                return new BigInteger("0");
            } else {
                return r;
            }
        }

        /**
         * Legt den Wert der r-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setR(BigInteger value) {
            this.r = value;
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

}
