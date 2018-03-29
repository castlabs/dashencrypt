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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import org.w3c.dom.Element;


/**
 * <p>Java-Klasse fxFCr RepresentationBaseType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="RepresentationBaseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="FramePacking" type="{urn:mpeg:dash:schema:mpd:2011}DescriptorType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="AudioChannelConfiguration" type="{urn:mpeg:dash:schema:mpd:2011}DescriptorType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="ContentProtection" type="{urn:mpeg:dash:schema:mpd:2011}DescriptorType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="EssentialProperty" type="{urn:mpeg:dash:schema:mpd:2011}DescriptorType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="SupplementalProperty" type="{urn:mpeg:dash:schema:mpd:2011}DescriptorType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="InbandEventStream" type="{urn:mpeg:dash:schema:mpd:2011}DescriptorType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="profiles" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="width" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="height" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="sar" type="{urn:mpeg:dash:schema:mpd:2011}RatioType" /&gt;
 *       &lt;attribute name="frameRate" type="{urn:mpeg:dash:schema:mpd:2011}FrameRateType" /&gt;
 *       &lt;attribute name="audioSamplingRate" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="mimeType" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="segmentProfiles" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="codecs" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="maximumSAPPeriod" type="{http://www.w3.org/2001/XMLSchema}double" /&gt;
 *       &lt;attribute name="startWithSAP" type="{urn:mpeg:dash:schema:mpd:2011}SAPType" /&gt;
 *       &lt;attribute name="maxPlayoutRate" type="{http://www.w3.org/2001/XMLSchema}double" /&gt;
 *       &lt;attribute name="codingDependency" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="scanType" type="{urn:mpeg:dash:schema:mpd:2011}VideoScanType" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RepresentationBaseType", propOrder = {
    "framePacking",
    "audioChannelConfiguration",
    "contentProtection",
    "essentialProperty",
    "supplementalProperty",
    "inbandEventStream",
    "any"
})
@XmlSeeAlso({
    AdaptationSetType.class,
    RepresentationType.class,
    SubRepresentationType.class
})
public class RepresentationBaseType {

    @XmlElement(name = "FramePacking")
    protected List<DescriptorType> framePacking;
    @XmlElement(name = "AudioChannelConfiguration")
    protected List<DescriptorType> audioChannelConfiguration;
    @XmlElement(name = "ContentProtection")
    protected List<DescriptorType> contentProtection;
    @XmlElement(name = "EssentialProperty")
    protected List<DescriptorType> essentialProperty;
    @XmlElement(name = "SupplementalProperty")
    protected List<DescriptorType> supplementalProperty;
    @XmlElement(name = "InbandEventStream")
    protected List<DescriptorType> inbandEventStream;
    @XmlAnyElement(lax = true)
    protected List<Object> any;
    @XmlAttribute(name = "profiles")
    protected String profiles;
    @XmlAttribute(name = "width")
    @XmlSchemaType(name = "unsignedInt")
    protected Long width;
    @XmlAttribute(name = "height")
    @XmlSchemaType(name = "unsignedInt")
    protected Long height;
    @XmlAttribute(name = "sar")
    protected String sar;
    @XmlAttribute(name = "frameRate")
    protected String frameRate;
    @XmlAttribute(name = "audioSamplingRate")
    protected String audioSamplingRate;
    @XmlAttribute(name = "mimeType")
    protected String mimeType;
    @XmlAttribute(name = "segmentProfiles")
    protected String segmentProfiles;
    @XmlAttribute(name = "codecs")
    protected String codecs;
    @XmlAttribute(name = "maximumSAPPeriod")
    protected Double maximumSAPPeriod;
    @XmlAttribute(name = "startWithSAP")
    protected Long startWithSAP;
    @XmlAttribute(name = "maxPlayoutRate")
    protected Double maxPlayoutRate;
    @XmlAttribute(name = "codingDependency")
    protected Boolean codingDependency;
    @XmlAttribute(name = "scanType")
    protected VideoScanType scanType;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the framePacking property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the framePacking property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFramePacking().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DescriptorType }
     * 
     * 
     */
    public List<DescriptorType> getFramePacking() {
        if (framePacking == null) {
            framePacking = new ArrayList<DescriptorType>();
        }
        return this.framePacking;
    }

    /**
     * Gets the value of the audioChannelConfiguration property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the audioChannelConfiguration property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAudioChannelConfiguration().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DescriptorType }
     * 
     * 
     */
    public List<DescriptorType> getAudioChannelConfiguration() {
        if (audioChannelConfiguration == null) {
            audioChannelConfiguration = new ArrayList<DescriptorType>();
        }
        return this.audioChannelConfiguration;
    }

    /**
     * Gets the value of the contentProtection property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the contentProtection property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getContentProtection().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DescriptorType }
     * 
     * 
     */
    public List<DescriptorType> getContentProtection() {
        if (contentProtection == null) {
            contentProtection = new ArrayList<DescriptorType>();
        }
        return this.contentProtection;
    }

    /**
     * Gets the value of the essentialProperty property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the essentialProperty property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEssentialProperty().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DescriptorType }
     * 
     * 
     */
    public List<DescriptorType> getEssentialProperty() {
        if (essentialProperty == null) {
            essentialProperty = new ArrayList<DescriptorType>();
        }
        return this.essentialProperty;
    }

    /**
     * Gets the value of the supplementalProperty property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the supplementalProperty property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSupplementalProperty().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DescriptorType }
     * 
     * 
     */
    public List<DescriptorType> getSupplementalProperty() {
        if (supplementalProperty == null) {
            supplementalProperty = new ArrayList<DescriptorType>();
        }
        return this.supplementalProperty;
    }

    /**
     * Gets the value of the inbandEventStream property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the inbandEventStream property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInbandEventStream().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DescriptorType }
     * 
     * 
     */
    public List<DescriptorType> getInbandEventStream() {
        if (inbandEventStream == null) {
            inbandEventStream = new ArrayList<DescriptorType>();
        }
        return this.inbandEventStream;
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
     * Ruft den Wert der width-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getWidth() {
        return width;
    }

    /**
     * Legt den Wert der width-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setWidth(Long value) {
        this.width = value;
    }

    /**
     * Ruft den Wert der height-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getHeight() {
        return height;
    }

    /**
     * Legt den Wert der height-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setHeight(Long value) {
        this.height = value;
    }

    /**
     * Ruft den Wert der sar-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSar() {
        return sar;
    }

    /**
     * Legt den Wert der sar-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSar(String value) {
        this.sar = value;
    }

    /**
     * Ruft den Wert der frameRate-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFrameRate() {
        return frameRate;
    }

    /**
     * Legt den Wert der frameRate-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFrameRate(String value) {
        this.frameRate = value;
    }

    /**
     * Ruft den Wert der audioSamplingRate-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAudioSamplingRate() {
        return audioSamplingRate;
    }

    /**
     * Legt den Wert der audioSamplingRate-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAudioSamplingRate(String value) {
        this.audioSamplingRate = value;
    }

    /**
     * Ruft den Wert der mimeType-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Legt den Wert der mimeType-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMimeType(String value) {
        this.mimeType = value;
    }

    /**
     * Ruft den Wert der segmentProfiles-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSegmentProfiles() {
        return segmentProfiles;
    }

    /**
     * Legt den Wert der segmentProfiles-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSegmentProfiles(String value) {
        this.segmentProfiles = value;
    }

    /**
     * Ruft den Wert der codecs-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodecs() {
        return codecs;
    }

    /**
     * Legt den Wert der codecs-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCodecs(String value) {
        this.codecs = value;
    }

    /**
     * Ruft den Wert der maximumSAPPeriod-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getMaximumSAPPeriod() {
        return maximumSAPPeriod;
    }

    /**
     * Legt den Wert der maximumSAPPeriod-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setMaximumSAPPeriod(Double value) {
        this.maximumSAPPeriod = value;
    }

    /**
     * Ruft den Wert der startWithSAP-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getStartWithSAP() {
        return startWithSAP;
    }

    /**
     * Legt den Wert der startWithSAP-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setStartWithSAP(Long value) {
        this.startWithSAP = value;
    }

    /**
     * Ruft den Wert der maxPlayoutRate-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getMaxPlayoutRate() {
        return maxPlayoutRate;
    }

    /**
     * Legt den Wert der maxPlayoutRate-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setMaxPlayoutRate(Double value) {
        this.maxPlayoutRate = value;
    }

    /**
     * Ruft den Wert der codingDependency-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isCodingDependency() {
        return codingDependency;
    }

    /**
     * Legt den Wert der codingDependency-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setCodingDependency(Boolean value) {
        this.codingDependency = value;
    }

    /**
     * Ruft den Wert der scanType-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link VideoScanType }
     *     
     */
    public VideoScanType getScanType() {
        return scanType;
    }

    /**
     * Legt den Wert der scanType-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link VideoScanType }
     *     
     */
    public void setScanType(VideoScanType value) {
        this.scanType = value;
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
