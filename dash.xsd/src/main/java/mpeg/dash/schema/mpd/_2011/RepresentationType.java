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


/**
 * <p>Java-Klasse fxFCr RepresentationType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="RepresentationType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:mpeg:dash:schema:mpd:2011}RepresentationBaseType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="BaseURL" type="{urn:mpeg:dash:schema:mpd:2011}BaseURLType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="SubRepresentation" type="{urn:mpeg:dash:schema:mpd:2011}SubRepresentationType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="SegmentBase" type="{urn:mpeg:dash:schema:mpd:2011}SegmentBaseType" minOccurs="0"/&gt;
 *         &lt;element name="SegmentList" type="{urn:mpeg:dash:schema:mpd:2011}SegmentListType" minOccurs="0"/&gt;
 *         &lt;element name="SegmentTemplate" type="{urn:mpeg:dash:schema:mpd:2011}SegmentTemplateType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="id" use="required" type="{urn:mpeg:dash:schema:mpd:2011}StringNoWhitespaceType" /&gt;
 *       &lt;attribute name="bandwidth" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="qualityRanking" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="dependencyId" type="{urn:mpeg:dash:schema:mpd:2011}StringVectorType" /&gt;
 *       &lt;attribute name="mediaStreamStructureId" type="{urn:mpeg:dash:schema:mpd:2011}StringVectorType" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RepresentationType", propOrder = {
    "baseURL",
    "subRepresentation",
    "segmentBase",
    "segmentList",
    "segmentTemplate"
})
public class RepresentationType
    extends RepresentationBaseType
{

    @XmlElement(name = "BaseURL")
    protected List<BaseURLType> baseURL;
    @XmlElement(name = "SubRepresentation")
    protected List<SubRepresentationType> subRepresentation;
    @XmlElement(name = "SegmentBase")
    protected SegmentBaseType segmentBase;
    @XmlElement(name = "SegmentList")
    protected SegmentListType segmentList;
    @XmlElement(name = "SegmentTemplate")
    protected SegmentTemplateType segmentTemplate;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "bandwidth", required = true)
    @XmlSchemaType(name = "unsignedInt")
    protected long bandwidth;
    @XmlAttribute(name = "qualityRanking")
    @XmlSchemaType(name = "unsignedInt")
    protected Long qualityRanking;
    @XmlAttribute(name = "dependencyId")
    protected List<String> dependencyId;
    @XmlAttribute(name = "mediaStreamStructureId")
    protected List<String> mediaStreamStructureId;

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
     * Gets the value of the subRepresentation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the subRepresentation property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSubRepresentation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SubRepresentationType }
     * 
     * 
     */
    public List<SubRepresentationType> getSubRepresentation() {
        if (subRepresentation == null) {
            subRepresentation = new ArrayList<SubRepresentationType>();
        }
        return this.subRepresentation;
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
     * Ruft den Wert der bandwidth-Eigenschaft ab.
     * 
     */
    public long getBandwidth() {
        return bandwidth;
    }

    /**
     * Legt den Wert der bandwidth-Eigenschaft fest.
     * 
     */
    public void setBandwidth(long value) {
        this.bandwidth = value;
    }

    /**
     * Ruft den Wert der qualityRanking-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getQualityRanking() {
        return qualityRanking;
    }

    /**
     * Legt den Wert der qualityRanking-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setQualityRanking(Long value) {
        this.qualityRanking = value;
    }

    /**
     * Gets the value of the dependencyId property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dependencyId property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDependencyId().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getDependencyId() {
        if (dependencyId == null) {
            dependencyId = new ArrayList<String>();
        }
        return this.dependencyId;
    }

    /**
     * Gets the value of the mediaStreamStructureId property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mediaStreamStructureId property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMediaStreamStructureId().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getMediaStreamStructureId() {
        if (mediaStreamStructureId == null) {
            mediaStreamStructureId = new ArrayList<String>();
        }
        return this.mediaStreamStructureId;
    }

}
