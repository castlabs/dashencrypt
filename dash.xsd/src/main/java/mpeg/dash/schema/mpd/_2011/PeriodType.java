//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0-b170531.0717 generiert 
// Siehe <a href="https://jaxb.java.net/">https://jaxb.java.net/</a> 
// xc4nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2018.03.28 um 10:20:11 AM CEST 
//


package mpeg.dash.schema.mpd._2011;

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
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import org.w3._1999.xlink.ActuateType;
import org.w3c.dom.Element;


/**
 * <p>Java-Klasse fxFCr PeriodType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="PeriodType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="BaseURL" type="{urn:mpeg:dash:schema:mpd:2011}BaseURLType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="SegmentBase" type="{urn:mpeg:dash:schema:mpd:2011}SegmentBaseType" minOccurs="0"/&gt;
 *         &lt;element name="SegmentList" type="{urn:mpeg:dash:schema:mpd:2011}SegmentListType" minOccurs="0"/&gt;
 *         &lt;element name="SegmentTemplate" type="{urn:mpeg:dash:schema:mpd:2011}SegmentTemplateType" minOccurs="0"/&gt;
 *         &lt;element name="AssetIdentifier" type="{urn:mpeg:dash:schema:mpd:2011}DescriptorType" minOccurs="0"/&gt;
 *         &lt;element name="EventStream" type="{urn:mpeg:dash:schema:mpd:2011}EventStreamType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="AdaptationSet" type="{urn:mpeg:dash:schema:mpd:2011}AdaptationSetType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="Subset" type="{urn:mpeg:dash:schema:mpd:2011}SubsetType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute ref="{http://www.w3.org/1999/xlink}href"/&gt;
 *       &lt;attribute ref="{http://www.w3.org/1999/xlink}actuate"/&gt;
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="start" type="{http://www.w3.org/2001/XMLSchema}duration" /&gt;
 *       &lt;attribute name="duration" type="{http://www.w3.org/2001/XMLSchema}duration" /&gt;
 *       &lt;attribute name="bitstreamSwitching" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PeriodType", propOrder = {
    "baseURL",
    "segmentBase",
    "segmentList",
    "segmentTemplate",
    "assetIdentifier",
    "eventStream",
    "adaptationSet",
    "subset",
    "any"
})
public class PeriodType {

    @XmlElement(name = "BaseURL")
    protected List<BaseURLType> baseURL;
    @XmlElement(name = "SegmentBase")
    protected SegmentBaseType segmentBase;
    @XmlElement(name = "SegmentList")
    protected SegmentListType segmentList;
    @XmlElement(name = "SegmentTemplate")
    protected SegmentTemplateType segmentTemplate;
    @XmlElement(name = "AssetIdentifier")
    protected DescriptorType assetIdentifier;
    @XmlElement(name = "EventStream")
    protected List<EventStreamType> eventStream;
    @XmlElement(name = "AdaptationSet")
    protected List<AdaptationSetType> adaptationSet;
    @XmlElement(name = "Subset")
    protected List<SubsetType> subset;
    @XmlAnyElement(lax = true)
    protected List<Object> any;
    @XmlAttribute(name = "href", namespace = "http://www.w3.org/1999/xlink")
    protected String href;
    @XmlAttribute(name = "actuate", namespace = "http://www.w3.org/1999/xlink")
    protected ActuateType actuate;
    @XmlAttribute(name = "id")
    protected String id;
    @XmlAttribute(name = "start")
    protected Duration start;
    @XmlAttribute(name = "duration")
    protected Duration duration;
    @XmlAttribute(name = "bitstreamSwitching")
    protected Boolean bitstreamSwitching;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the baseURL property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the baseURL property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBaseURL().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link BaseURLType }
     * 
     * 
     */
    public List<BaseURLType> getBaseURL() {
        if (baseURL == null) {
            baseURL = new ArrayList<BaseURLType>();
        }
        return this.baseURL;
    }

    /**
     * Ruft den Wert der segmentBase-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link SegmentBaseType }
     *     
     */
    public SegmentBaseType getSegmentBase() {
        return segmentBase;
    }

    /**
     * Legt den Wert der segmentBase-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link SegmentBaseType }
     *     
     */
    public void setSegmentBase(SegmentBaseType value) {
        this.segmentBase = value;
    }

    /**
     * Ruft den Wert der segmentList-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link SegmentListType }
     *     
     */
    public SegmentListType getSegmentList() {
        return segmentList;
    }

    /**
     * Legt den Wert der segmentList-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link SegmentListType }
     *     
     */
    public void setSegmentList(SegmentListType value) {
        this.segmentList = value;
    }

    /**
     * Ruft den Wert der segmentTemplate-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link SegmentTemplateType }
     *     
     */
    public SegmentTemplateType getSegmentTemplate() {
        return segmentTemplate;
    }

    /**
     * Legt den Wert der segmentTemplate-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link SegmentTemplateType }
     *     
     */
    public void setSegmentTemplate(SegmentTemplateType value) {
        this.segmentTemplate = value;
    }

    /**
     * Ruft den Wert der assetIdentifier-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link DescriptorType }
     *     
     */
    public DescriptorType getAssetIdentifier() {
        return assetIdentifier;
    }

    /**
     * Legt den Wert der assetIdentifier-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link DescriptorType }
     *     
     */
    public void setAssetIdentifier(DescriptorType value) {
        this.assetIdentifier = value;
    }

    /**
     * Gets the value of the eventStream property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the eventStream property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEventStream().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link EventStreamType }
     * 
     * 
     */
    public List<EventStreamType> getEventStream() {
        if (eventStream == null) {
            eventStream = new ArrayList<EventStreamType>();
        }
        return this.eventStream;
    }

    /**
     * Gets the value of the adaptationSet property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the adaptationSet property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAdaptationSet().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AdaptationSetType }
     * 
     * 
     */
    public List<AdaptationSetType> getAdaptationSet() {
        if (adaptationSet == null) {
            adaptationSet = new ArrayList<AdaptationSetType>();
        }
        return this.adaptationSet;
    }

    /**
     * Gets the value of the subset property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the subset property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSubset().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SubsetType }
     * 
     * 
     */
    public List<SubsetType> getSubset() {
        if (subset == null) {
            subset = new ArrayList<SubsetType>();
        }
        return this.subset;
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
     * Ruft den Wert der href-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHref() {
        return href;
    }

    /**
     * Legt den Wert der href-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHref(String value) {
        this.href = value;
    }

    /**
     * Ruft den Wert der actuate-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link ActuateType }
     *     
     */
    public ActuateType getActuate() {
        if (actuate == null) {
            return ActuateType.ON_REQUEST;
        } else {
            return actuate;
        }
    }

    /**
     * Legt den Wert der actuate-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link ActuateType }
     *     
     */
    public void setActuate(ActuateType value) {
        this.actuate = value;
    }

    /**
     * Ruft den Wert der id-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Legt den Wert der id-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Ruft den Wert der start-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getStart() {
        return start;
    }

    /**
     * Legt den Wert der start-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setStart(Duration value) {
        this.start = value;
    }

    /**
     * Ruft den Wert der duration-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Legt den Wert der duration-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setDuration(Duration value) {
        this.duration = value;
    }

    /**
     * Ruft den Wert der bitstreamSwitching-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isBitstreamSwitching() {
        if (bitstreamSwitching == null) {
            return false;
        } else {
            return bitstreamSwitching;
        }
    }

    /**
     * Legt den Wert der bitstreamSwitching-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBitstreamSwitching(Boolean value) {
        this.bitstreamSwitching = value;
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
