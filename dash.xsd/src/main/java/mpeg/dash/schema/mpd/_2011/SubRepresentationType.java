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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse fxFCr SubRepresentationType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="SubRepresentationType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:mpeg:dash:schema:mpd:2011}RepresentationBaseType"&gt;
 *       &lt;attribute name="level" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="dependencyLevel" type="{urn:mpeg:dash:schema:mpd:2011}UIntVectorType" /&gt;
 *       &lt;attribute name="bandwidth" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" /&gt;
 *       &lt;attribute name="contentComponent" type="{urn:mpeg:dash:schema:mpd:2011}StringVectorType" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SubRepresentationType")
public class SubRepresentationType
    extends RepresentationBaseType
{

    @XmlAttribute(name = "level")
    @XmlSchemaType(name = "unsignedInt")
    protected Long level;
    @XmlAttribute(name = "dependencyLevel")
    protected List<Long> dependencyLevel;
    @XmlAttribute(name = "bandwidth")
    @XmlSchemaType(name = "unsignedInt")
    protected Long bandwidth;
    @XmlAttribute(name = "contentComponent")
    protected List<String> contentComponent;

    /**
     * Ruft den Wert der level-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getLevel() {
        return level;
    }

    /**
     * Legt den Wert der level-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setLevel(Long value) {
        this.level = value;
    }

    /**
     * Gets the value of the dependencyLevel property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dependencyLevel property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDependencyLevel().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Long }
     * 
     * 
     */
    public List<Long> getDependencyLevel() {
        if (dependencyLevel == null) {
            dependencyLevel = new ArrayList<Long>();
        }
        return this.dependencyLevel;
    }

    /**
     * Ruft den Wert der bandwidth-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getBandwidth() {
        return bandwidth;
    }

    /**
     * Legt den Wert der bandwidth-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setBandwidth(Long value) {
        this.bandwidth = value;
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
     * {@link String }
     * 
     * 
     */
    public List<String> getContentComponent() {
        if (contentComponent == null) {
            contentComponent = new ArrayList<String>();
        }
        return this.contentComponent;
    }

}
