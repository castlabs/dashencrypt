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
import javax.xml.bind.annotation.*;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import org.w3c.dom.Element;


/**
 * <p>Java-Klasse fxFCr MPDtype complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="MPDtype"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ProgramInformation" type="{urn:mpeg:dash:schema:mpd:2011}ProgramInformationType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="BaseURL" type="{urn:mpeg:dash:schema:mpd:2011}BaseURLType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="Location" type="{http://www.w3.org/2001/XMLSchema}anyURI" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="Period" type="{urn:mpeg:dash:schema:mpd:2011}PeriodType" maxOccurs="unbounded"/&gt;
 *         &lt;element name="Metrics" type="{urn:mpeg:dash:schema:mpd:2011}MetricsType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="profiles" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="type" type="{urn:mpeg:dash:schema:mpd:2011}PresentationType" default="static" /&gt;
 *       &lt;attribute name="availabilityStartTime" type="{http://www.w3.org/2001/XMLSchema}dateTime" /&gt;
 *       &lt;attribute name="availabilityEndTime" type="{http://www.w3.org/2001/XMLSchema}dateTime" /&gt;
 *       &lt;attribute name="publishTime" type="{http://www.w3.org/2001/XMLSchema}dateTime" /&gt;
 *       &lt;attribute name="mediaPresentationDuration" type="{http://www.w3.org/2001/XMLSchema}duration" /&gt;
 *       &lt;attribute name="minimumUpdatePeriod" type="{http://www.w3.org/2001/XMLSchema}duration" /&gt;
 *       &lt;attribute name="minBufferTime" use="required" type="{http://www.w3.org/2001/XMLSchema}duration" /&gt;
 *       &lt;attribute name="timeShiftBufferDepth" type="{http://www.w3.org/2001/XMLSchema}duration" /&gt;
 *       &lt;attribute name="suggestedPresentationDelay" type="{http://www.w3.org/2001/XMLSchema}duration" /&gt;
 *       &lt;attribute name="maxSegmentDuration" type="{http://www.w3.org/2001/XMLSchema}duration" /&gt;
 *       &lt;attribute name="maxSubsegmentDuration" type="{http://www.w3.org/2001/XMLSchema}duration" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MPDtype", propOrder = {
    "programInformation",
    "baseURL",
    "location",
    "period",
    "metrics",
    "any"
})
public class MPDtype {

    @XmlElement(name = "ProgramInformation")
    protected List<ProgramInformationType> programInformation;
    @XmlElement(name = "BaseURL")
    protected List<BaseURLType> baseURL;
    @XmlElement(name = "Location")
    @XmlSchemaType(name = "anyURI")
    protected List<String> location;
    @XmlElement(name = "Period", required = true)
    protected List<PeriodType> period;
    @XmlElement(name = "Metrics")
    protected List<MetricsType> metrics;
    @XmlAnyElement(lax = true)
    protected List<Object> any;
    @XmlAttribute(name = "id")
    protected String id;
    @XmlAttribute(name = "profiles", required = true)
    protected String profiles;
    @XmlAttribute(name = "type")
    protected PresentationType type;
    @XmlAttribute(name = "availabilityStartTime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar availabilityStartTime;
    @XmlAttribute(name = "availabilityEndTime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar availabilityEndTime;
    @XmlAttribute(name = "publishTime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar publishTime;
    @XmlAttribute(name = "mediaPresentationDuration")
    protected Duration mediaPresentationDuration;
    @XmlAttribute(name = "minimumUpdatePeriod")
    protected Duration minimumUpdatePeriod;
    @XmlAttribute(name = "minBufferTime", required = true)
    protected Duration minBufferTime;
    @XmlAttribute(name = "timeShiftBufferDepth")
    protected Duration timeShiftBufferDepth;
    @XmlAttribute(name = "suggestedPresentationDelay")
    protected Duration suggestedPresentationDelay;
    @XmlAttribute(name = "maxSegmentDuration")
    protected Duration maxSegmentDuration;
    @XmlAttribute(name = "maxSubsegmentDuration")
    protected Duration maxSubsegmentDuration;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the programInformation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the programInformation property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProgramInformation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ProgramInformationType }
     * 
     * 
     */
    public List<ProgramInformationType> getProgramInformation() {
        if (programInformation == null) {
            programInformation = new ArrayList<ProgramInformationType>();
        }
        return this.programInformation;
    }

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
     * Gets the value of the location property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the location property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLocation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getLocation() {
        if (location == null) {
            location = new ArrayList<String>();
        }
        return this.location;
    }

    /**
     * Gets the value of the period property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the period property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPeriod().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PeriodType }
     * 
     * 
     */
    public List<PeriodType> getPeriod() {
        if (period == null) {
            period = new ArrayList<PeriodType>();
        }
        return this.period;
    }

    /**
     * Gets the value of the metrics property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the metrics property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMetrics().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MetricsType }
     * 
     * 
     */
    public List<MetricsType> getMetrics() {
        if (metrics == null) {
            metrics = new ArrayList<MetricsType>();
        }
        return this.metrics;
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
     * Ruft den Wert der profiles-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProfiles() {
        return profiles;
    }

    /**
     * Legt den Wert der profiles-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProfiles(String value) {
        this.profiles = value;
    }

    /**
     * Ruft den Wert der type-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link PresentationType }
     *     
     */
    public PresentationType getType() {
        if (type == null) {
            return PresentationType.STATIC;
        } else {
            return type;
        }
    }

    /**
     * Legt den Wert der type-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link PresentationType }
     *     
     */
    public void setType(PresentationType value) {
        this.type = value;
    }

    /**
     * Ruft den Wert der availabilityStartTime-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getAvailabilityStartTime() {
        return availabilityStartTime;
    }

    /**
     * Legt den Wert der availabilityStartTime-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setAvailabilityStartTime(XMLGregorianCalendar value) {
        this.availabilityStartTime = value;
    }

    /**
     * Ruft den Wert der availabilityEndTime-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getAvailabilityEndTime() {
        return availabilityEndTime;
    }

    /**
     * Legt den Wert der availabilityEndTime-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setAvailabilityEndTime(XMLGregorianCalendar value) {
        this.availabilityEndTime = value;
    }

    /**
     * Ruft den Wert der publishTime-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getPublishTime() {
        return publishTime;
    }

    /**
     * Legt den Wert der publishTime-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setPublishTime(XMLGregorianCalendar value) {
        this.publishTime = value;
    }

    /**
     * Ruft den Wert der mediaPresentationDuration-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getMediaPresentationDuration() {
        return mediaPresentationDuration;
    }

    /**
     * Legt den Wert der mediaPresentationDuration-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setMediaPresentationDuration(Duration value) {
        this.mediaPresentationDuration = value;
    }

    /**
     * Ruft den Wert der minimumUpdatePeriod-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getMinimumUpdatePeriod() {
        return minimumUpdatePeriod;
    }

    /**
     * Legt den Wert der minimumUpdatePeriod-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setMinimumUpdatePeriod(Duration value) {
        this.minimumUpdatePeriod = value;
    }

    /**
     * Ruft den Wert der minBufferTime-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getMinBufferTime() {
        return minBufferTime;
    }

    /**
     * Legt den Wert der minBufferTime-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setMinBufferTime(Duration value) {
        this.minBufferTime = value;
    }

    /**
     * Ruft den Wert der timeShiftBufferDepth-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getTimeShiftBufferDepth() {
        return timeShiftBufferDepth;
    }

    /**
     * Legt den Wert der timeShiftBufferDepth-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setTimeShiftBufferDepth(Duration value) {
        this.timeShiftBufferDepth = value;
    }

    /**
     * Ruft den Wert der suggestedPresentationDelay-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getSuggestedPresentationDelay() {
        return suggestedPresentationDelay;
    }

    /**
     * Legt den Wert der suggestedPresentationDelay-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setSuggestedPresentationDelay(Duration value) {
        this.suggestedPresentationDelay = value;
    }

    /**
     * Ruft den Wert der maxSegmentDuration-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getMaxSegmentDuration() {
        return maxSegmentDuration;
    }

    /**
     * Legt den Wert der maxSegmentDuration-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setMaxSegmentDuration(Duration value) {
        this.maxSegmentDuration = value;
    }

    /**
     * Ruft den Wert der maxSubsegmentDuration-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getMaxSubsegmentDuration() {
        return maxSubsegmentDuration;
    }

    /**
     * Legt den Wert der maxSubsegmentDuration-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setMaxSubsegmentDuration(Duration value) {
        this.maxSubsegmentDuration = value;
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
