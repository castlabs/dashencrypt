//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0-b170531.0717 generiert 
// Siehe <a href="https://jaxb.java.net/">https://jaxb.java.net/</a> 
// xc4nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2018.03.28 um 10:20:11 AM CEST 
//


package mpeg.dash.schema.mpd._2011;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the mpeg.dash.schema.mpd._2011 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _MPD_QNAME = new QName("urn:mpeg:dash:schema:mpd:2011", "MPD");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: mpeg.dash.schema.mpd._2011
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SegmentTimelineType }
     * 
     */
    public SegmentTimelineType createSegmentTimelineType() {
        return new SegmentTimelineType();
    }

    /**
     * Create an instance of {@link MPDtype }
     * 
     */
    public MPDtype createMPDtype() {
        return new MPDtype();
    }

    /**
     * Create an instance of {@link PeriodType }
     * 
     */
    public PeriodType createPeriodType() {
        return new PeriodType();
    }

    /**
     * Create an instance of {@link EventStreamType }
     * 
     */
    public EventStreamType createEventStreamType() {
        return new EventStreamType();
    }

    /**
     * Create an instance of {@link EventType }
     * 
     */
    public EventType createEventType() {
        return new EventType();
    }

    /**
     * Create an instance of {@link AdaptationSetType }
     * 
     */
    public AdaptationSetType createAdaptationSetType() {
        return new AdaptationSetType();
    }

    /**
     * Create an instance of {@link ContentComponentType }
     * 
     */
    public ContentComponentType createContentComponentType() {
        return new ContentComponentType();
    }

    /**
     * Create an instance of {@link RepresentationType }
     * 
     */
    public RepresentationType createRepresentationType() {
        return new RepresentationType();
    }

    /**
     * Create an instance of {@link SubRepresentationType }
     * 
     */
    public SubRepresentationType createSubRepresentationType() {
        return new SubRepresentationType();
    }

    /**
     * Create an instance of {@link RepresentationBaseType }
     * 
     */
    public RepresentationBaseType createRepresentationBaseType() {
        return new RepresentationBaseType();
    }

    /**
     * Create an instance of {@link SubsetType }
     * 
     */
    public SubsetType createSubsetType() {
        return new SubsetType();
    }

    /**
     * Create an instance of {@link SegmentBaseType }
     * 
     */
    public SegmentBaseType createSegmentBaseType() {
        return new SegmentBaseType();
    }

    /**
     * Create an instance of {@link MultipleSegmentBaseType }
     * 
     */
    public MultipleSegmentBaseType createMultipleSegmentBaseType() {
        return new MultipleSegmentBaseType();
    }

    /**
     * Create an instance of {@link URLType }
     * 
     */
    public URLType createURLType() {
        return new URLType();
    }

    /**
     * Create an instance of {@link SegmentListType }
     * 
     */
    public SegmentListType createSegmentListType() {
        return new SegmentListType();
    }

    /**
     * Create an instance of {@link SegmentURLType }
     * 
     */
    public SegmentURLType createSegmentURLType() {
        return new SegmentURLType();
    }

    /**
     * Create an instance of {@link SegmentTemplateType }
     * 
     */
    public SegmentTemplateType createSegmentTemplateType() {
        return new SegmentTemplateType();
    }

    /**
     * Create an instance of {@link BaseURLType }
     * 
     */
    public BaseURLType createBaseURLType() {
        return new BaseURLType();
    }

    /**
     * Create an instance of {@link ProgramInformationType }
     * 
     */
    public ProgramInformationType createProgramInformationType() {
        return new ProgramInformationType();
    }

    /**
     * Create an instance of {@link DescriptorType }
     * 
     */
    public DescriptorType createDescriptorType() {
        return new DescriptorType();
    }

    /**
     * Create an instance of {@link MetricsType }
     * 
     */
    public MetricsType createMetricsType() {
        return new MetricsType();
    }

    /**
     * Create an instance of {@link RangeType }
     * 
     */
    public RangeType createRangeType() {
        return new RangeType();
    }

    /**
     * Create an instance of {@link SegmentTimelineType.S }
     * 
     */
    public SegmentTimelineType.S createSegmentTimelineTypeS() {
        return new SegmentTimelineType.S();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MPDtype }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link MPDtype }{@code >}
     */
    @XmlElementDecl(namespace = "urn:mpeg:dash:schema:mpd:2011", name = "MPD")
    public JAXBElement<MPDtype> createMPD(MPDtype value) {
        return new JAXBElement<MPDtype>(_MPD_QNAME, MPDtype.class, null, value);
    }

}
