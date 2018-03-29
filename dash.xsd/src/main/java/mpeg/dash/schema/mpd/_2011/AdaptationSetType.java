//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0-b170531.0717 generiert 
// Siehe <a href="https://jaxb.java.net/">https://jaxb.java.net/</a> 
// xc4nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2018.03.28 um 10:20:11 AM CEST 
//


package mpeg.dash.schema.mpd._2011;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.w3._1999.xlink.ActuateType;


/**
 * <p>Java-Klasse fxFCr AdaptationSetType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="AdaptationSetType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:mpeg:dash:schema:mpd:2011}RepresentationBaseType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Accessibility" type="{urn:mpeg:dash:schema:mpd:2011}DescriptorType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="Role" type="{urn:mpeg:dash:schema:mpd:2011}DescriptorType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="Rating" type="{urn:mpeg:dash:schema:mpd:2011}DescriptorType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="Viewpoint" type="{urn:mpeg:dash:schema:mpd:2011}DescriptorType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="ContentComponent" type="{urn:mpeg:dash:schema:mpd:2011}ContentComponentType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="BaseURL" type="{urn:mpeg:dash:schema:mpd:2011}BaseURLType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="SegmentBase" type="{urn:mpeg:dash:schema:mpd:2011}SegmentBaseType" minOccurs="0"/&gt;
 *         &lt;element name="SegmentList" type="{urn:mpeg:dash:schema:mpd:2011}SegmentListType" minOccurs="0"/&gt;
 *         &lt;element name="SegmentTemplate" type="{urn:mpeg:dash:schema:mpd:2011}SegmentTemplateType" minOccurs="0"/&gt;
 *         &lt;element name="Representation" type="{urn:mpeg:dash:schema:mpd:2011}RepresentationType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute ref="{http://www.w3.org/1999/xlink}href"/&gt;
 *       &lt;attribute ref="{http://www.w3.org/1999/xlink}actuate"/&gt;
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="group" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="lang" type="{http://www.w3.org/2001/XMLSchema}language" /&gt;
 *       &lt;attribute name="contentType" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="par" type="{urn:mpeg:dash:schema:mpd:2011}RatioType" /&gt;
 *       &lt;attribute name="minBandwidth" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="maxBandwidth" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="minWidth" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="maxWidth" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="minHeight" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="maxHeight" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="minFrameRate" type="{urn:mpeg:dash:schema:mpd:2011}FrameRateType" /&gt;
 *       &lt;attribute name="maxFrameRate" type="{urn:mpeg:dash:schema:mpd:2011}FrameRateType" /&gt;
 *       &lt;attribute name="segmentAlignment" type="{urn:mpeg:dash:schema:mpd:2011}ConditionalUintType" default="false" /&gt;
 *       &lt;attribute name="subsegmentAlignment" type="{urn:mpeg:dash:schema:mpd:2011}ConditionalUintType" default="false" /&gt;
 *       &lt;attribute name="subsegmentStartsWithSAP" type="{urn:mpeg:dash:schema:mpd:2011}SAPType" default="0" /&gt;
 *       &lt;attribute name="bitstreamSwitching" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AdaptationSetType", propOrder = {
    "accessibility",
    "role",
    "rating",
    "viewpoint",
    "contentComponent",
    "baseURL",
    "segmentBase",
    "segmentList",
    "segmentTemplate",
    "representation"
})
public class AdaptationSetType
    extends RepresentationBaseType
{

    @XmlElement(name = "Accessibility")
    protected List<DescriptorType> accessibility;
    @XmlElement(name = "Role")
    protected List<DescriptorType> role;
    @XmlElement(name = "Rating")
    protected List<DescriptorType> rating;
    @XmlElement(name = "Viewpoint")
    protected List<DescriptorType> viewpoint;
    @XmlElement(name = "ContentComponent")
    protected List<ContentComponentType> contentComponent;
    @XmlElement(name = "BaseURL")
    protected List<BaseURLType> baseURL;
    @XmlElement(name = "SegmentBase")
    protected SegmentBaseType segmentBase;
    @XmlElement(name = "SegmentList")
    protected SegmentListType segmentList;
    @XmlElement(name = "SegmentTemplate")
    protected SegmentTemplateType segmentTemplate;
    @XmlElement(name = "Representation")
    protected List<RepresentationType> representation;
    @XmlAttribute(name = "href", namespace = "http://www.w3.org/1999/xlink")
    protected String href;
    @XmlAttribute(name = "actuate", namespace = "http://www.w3.org/1999/xlink")
    protected ActuateType actuate;
    @XmlAttribute(name = "id")
    @XmlSchemaType(name = "unsignedInt")
    protected Long id;
    @XmlAttribute(name = "group")
    @XmlSchemaType(name = "unsignedInt")
    protected Long group;
    @XmlAttribute(name = "lang")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "language")
    protected String lang;
    @XmlAttribute(name = "contentType")
    protected String contentType;
    @XmlAttribute(name = "par")
    protected String par;
    @XmlAttribute(name = "minBandwidth")
    @XmlSchemaType(name = "unsignedInt")
    protected Long minBandwidth;
    @XmlAttribute(name = "maxBandwidth")
    @XmlSchemaType(name = "unsignedInt")
    protected Long maxBandwidth;
    @XmlAttribute(name = "minWidth")
    @XmlSchemaType(name = "unsignedInt")
    protected Long minWidth;
    @XmlAttribute(name = "maxWidth")
    @XmlSchemaType(name = "unsignedInt")
    protected Long maxWidth;
    @XmlAttribute(name = "minHeight")
    @XmlSchemaType(name = "unsignedInt")
    protected Long minHeight;
    @XmlAttribute(name = "maxHeight")
    @XmlSchemaType(name = "unsignedInt")
    protected Long maxHeight;
    @XmlAttribute(name = "minFrameRate")
    protected String minFrameRate;
    @XmlAttribute(name = "maxFrameRate")
    protected String maxFrameRate;
    @XmlAttribute(name = "segmentAlignment")
    protected String segmentAlignment;
    @XmlAttribute(name = "subsegmentAlignment")
    protected String subsegmentAlignment;
    @XmlAttribute(name = "subsegmentStartsWithSAP")
    protected Long subsegmentStartsWithSAP;
    @XmlAttribute(name = "bitstreamSwitching")
    protected Boolean bitstreamSwitching;

    /**
     * Gets the value of the accessibility property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the accessibility property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccessibility().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DescriptorType }
     * 
     * 
     */
    public List<DescriptorType> getAccessibility() {
        if (accessibility == null) {
            accessibility = new ArrayList<DescriptorType>();
        }
        return this.accessibility;
    }

    /**
     * Gets the value of the role property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the role property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRole().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DescriptorType }
     * 
     * 
     */
    public List<DescriptorType> getRole() {
        if (role == null) {
            role = new ArrayList<DescriptorType>();
        }
        return this.role;
    }

    /**
     * Gets the value of the rating property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rating property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRating().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DescriptorType }
     * 
     * 
     */
    public List<DescriptorType> getRating() {
        if (rating == null) {
            rating = new ArrayList<DescriptorType>();
        }
        return this.rating;
    }

    /**
     * Gets the value of the viewpoint property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the viewpoint property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getViewpoint().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DescriptorType }
     * 
     * 
     */
    public List<DescriptorType> getViewpoint() {
        if (viewpoint == null) {
            viewpoint = new ArrayList<DescriptorType>();
        }
        return this.viewpoint;
    }

    /**
     * Gets the value of the contentComponent property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the contentComponent property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getContentComponent().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ContentComponentType }
     * 
     * 
     */
    public List<ContentComponentType> getContentComponent() {
        if (contentComponent == null) {
            contentComponent = new ArrayList<ContentComponentType>();
        }
        return this.contentComponent;
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
     * Gets the value of the representation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the representation property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRepresentation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RepresentationType }
     * 
     * 
     */
    public List<RepresentationType> getRepresentation() {
        if (representation == null) {
            representation = new ArrayList<RepresentationType>();
        }
        return this.representation;
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
     * Ruft den Wert der group-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getGroup() {
        return group;
    }

    /**
     * Legt den Wert der group-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setGroup(Long value) {
        this.group = value;
    }

    /**
     * Ruft den Wert der lang-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLang() {
        return lang;
    }

    /**
     * Legt den Wert der lang-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLang(String value) {
        this.lang = value;
    }

    /**
     * Ruft den Wert der contentType-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Legt den Wert der contentType-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContentType(String value) {
        this.contentType = value;
    }

    /**
     * Ruft den Wert der par-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPar() {
        return par;
    }

    /**
     * Legt den Wert der par-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPar(String value) {
        this.par = value;
    }

    /**
     * Ruft den Wert der minBandwidth-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMinBandwidth() {
        return minBandwidth;
    }

    /**
     * Legt den Wert der minBandwidth-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMinBandwidth(Long value) {
        this.minBandwidth = value;
    }

    /**
     * Ruft den Wert der maxBandwidth-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMaxBandwidth() {
        return maxBandwidth;
    }

    /**
     * Legt den Wert der maxBandwidth-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMaxBandwidth(Long value) {
        this.maxBandwidth = value;
    }

    /**
     * Ruft den Wert der minWidth-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMinWidth() {
        return minWidth;
    }

    /**
     * Legt den Wert der minWidth-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMinWidth(Long value) {
        this.minWidth = value;
    }

    /**
     * Ruft den Wert der maxWidth-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMaxWidth() {
        return maxWidth;
    }

    /**
     * Legt den Wert der maxWidth-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMaxWidth(Long value) {
        this.maxWidth = value;
    }

    /**
     * Ruft den Wert der minHeight-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMinHeight() {
        return minHeight;
    }

    /**
     * Legt den Wert der minHeight-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMinHeight(Long value) {
        this.minHeight = value;
    }

    /**
     * Ruft den Wert der maxHeight-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMaxHeight() {
        return maxHeight;
    }

    /**
     * Legt den Wert der maxHeight-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMaxHeight(Long value) {
        this.maxHeight = value;
    }

    /**
     * Ruft den Wert der minFrameRate-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMinFrameRate() {
        return minFrameRate;
    }

    /**
     * Legt den Wert der minFrameRate-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMinFrameRate(String value) {
        this.minFrameRate = value;
    }

    /**
     * Ruft den Wert der maxFrameRate-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMaxFrameRate() {
        return maxFrameRate;
    }

    /**
     * Legt den Wert der maxFrameRate-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMaxFrameRate(String value) {
        this.maxFrameRate = value;
    }

    /**
     * Ruft den Wert der segmentAlignment-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSegmentAlignment() {
        if (segmentAlignment == null) {
            return "false";
        } else {
            return segmentAlignment;
        }
    }

    /**
     * Legt den Wert der segmentAlignment-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSegmentAlignment(String value) {
        this.segmentAlignment = value;
    }

    /**
     * Ruft den Wert der subsegmentAlignment-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubsegmentAlignment() {
        if (subsegmentAlignment == null) {
            return "false";
        } else {
            return subsegmentAlignment;
        }
    }

    /**
     * Legt den Wert der subsegmentAlignment-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubsegmentAlignment(String value) {
        this.subsegmentAlignment = value;
    }

    /**
     * Ruft den Wert der subsegmentStartsWithSAP-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public long getSubsegmentStartsWithSAP() {
        if (subsegmentStartsWithSAP == null) {
            return  0L;
        } else {
            return subsegmentStartsWithSAP;
        }
    }

    /**
     * Legt den Wert der subsegmentStartsWithSAP-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setSubsegmentStartsWithSAP(Long value) {
        this.subsegmentStartsWithSAP = value;
    }

    /**
     * Ruft den Wert der bitstreamSwitching-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBitstreamSwitching() {
        return bitstreamSwitching;
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

}
