package com.castlabs.dash.dashfragmenter.representation;

import mpeg.dash.schema.mpd._2011.*;
import org.apache.commons.lang.math.Fraction;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjLongConsumer;

/**
 * Pushes common attributes and elements down to the parent element.
 */
public class ManifestOptimizer {
    static DatatypeFactory datatypeFactory = null;

    static {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }


    public static void optimize(MPDtype mpd) {
        adjustDuration(mpd);
        for (PeriodType periodType : mpd.getPeriod()) {
            for (AdaptationSetType adaptationSetType : periodType.getAdaptationSet()) {
                optimize(adaptationSetType);
                adjustMinMax(adaptationSetType, RepresentationType::getWidth, AdaptationSetType::setMinWidth, AdaptationSetType::setMaxWidth);
                adjustMinMax(adaptationSetType, RepresentationType::getHeight, AdaptationSetType::setMinHeight, AdaptationSetType::setMaxHeight);
                adjustMinMax(adaptationSetType, RepresentationType::getBandwidth, AdaptationSetType::setMinBandwidth, AdaptationSetType::setMaxBandwidth);
                adjustMinMaxFrameRate(adaptationSetType); // special handling cause the type is special (fraction)
            }
        }
    }

    public static void adjustDuration(MPDtype mpd) {
        Duration total = datatypeFactory.newDuration(0);
        for (PeriodType periodType : mpd.getPeriod()) {
            total = total.add(periodType.getDuration());
        }
        mpd.setMediaPresentationDuration(total);
    }

    public static void adjustMinMaxFrameRate(AdaptationSetType adaptationSetType) {
        List<RepresentationType> representations = adaptationSetType.getRepresentation();
        Fraction min = null, max = null;
        for (RepresentationType representationType : representations) {
            String frameRate = representationType.getFrameRate();
            if (frameRate != null) {
                Fraction f = Fraction.getFraction(frameRate);

                min = min == null || f.compareTo(min) < 0 ? f : min;
                max = max == null || f.compareTo(max) > 0 ? f : max;
            }
        }
        if (max != null && !min.equals(max)) { // min/max doesn't make sense when both values are the same
            adaptationSetType.setMinFrameRate(min.toString());
            adaptationSetType.setMaxFrameRate(max.toString());
        }
    }

    public static void adjustMinMax(AdaptationSetType adaptationSetType, Function<RepresentationType, Long> get, ObjLongConsumer<AdaptationSetType> setMin, ObjLongConsumer<AdaptationSetType> setMax) {
        List<RepresentationType> representations = adaptationSetType.getRepresentation();
        long min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (RepresentationType representationType : representations) {
            Long n = get.apply(representationType);
            if (n != null) {
                min = Math.min(n, min);
                max = Math.max(n, max);
            }
        }
        if (min != Integer.MAX_VALUE && min != max) {
            setMax.accept(adaptationSetType, max);
            setMin.accept(adaptationSetType, min);
        }
    }

    public static void optimize(AdaptationSetType adaptationSetType) {
        optimizeContentProtection(adaptationSetType);
        optimizeAttribute(adaptationSetType, RepresentationType::getMimeType, (RepresentationType r) -> r.setMimeType(null), (adaptationSetType2, value1) -> adaptationSetType2.setMimeType((String) value1));
        // This is commented out as customer P0132's chromecast player doesn't look for the codecs attribute on AdaptationSetBuilder level
        // The deal is to update the player before 2016. If you see this here in 2016 you can remove the comment and optimize
        // the codecs attribute as well.
        // optimizeAttribute(adaptationSetType, adaptationSetType.getRepresentationArray(), "codecs");
        optimizeAttribute(adaptationSetType, RepresentationType::getProfiles, (RepresentationType r) -> r.setProfiles(null), (adaptationSetType1, value) -> adaptationSetType1.setProfiles((String) value));
        optimizeAttribute(adaptationSetType, RepresentationType::getWidth, (RepresentationType r) -> r.setWidth(null), (adaptationSetType1, value) -> adaptationSetType1.setWidth((Long) value));
        optimizeAttribute(adaptationSetType, RepresentationType::getHeight, (RepresentationType r) -> r.setHeight(null), (adaptationSetType1, value) -> adaptationSetType1.setHeight((Long) value));
        optimizeAttribute(adaptationSetType, RepresentationType::getFrameRate, (RepresentationType r) -> r.setFrameRate(null), (adaptationSetType1, value) -> adaptationSetType1.setFrameRate((String) value));
        optimizeAttribute(adaptationSetType, RepresentationType::getSar, (RepresentationType r) -> r.setSar(null), (adaptationSetType1, value) -> adaptationSetType1.setSar((String) value));
    }


    private static void optimizeAttribute(AdaptationSetType adaptationSetType, Function<RepresentationType, Object> get, Consumer<RepresentationType> remove, BiConsumer<AdaptationSetType, Object> set) {
        Object v = null;
        for (RepresentationType representationType : adaptationSetType.getRepresentation()) {
            Object _v = get.apply(representationType);
            if (_v == null) {
                return; // no need to move it around when it doesn't exist
            }
            if (v == null || v.equals(_v)) {
                v = _v;
            } else {
                return;
            }
        }
        set.accept(adaptationSetType, v);
        for (RepresentationType representationType : adaptationSetType.getRepresentation()) {
            remove.accept(representationType);
        }
    }

    public static void optimizeContentProtection(AdaptationSetType parent) {
        List<DescriptorType> contentProtection = new ArrayList<>();
        for (RepresentationBaseType representationType : parent.getRepresentation()) {
            if (contentProtection.isEmpty()) {
                contentProtection.addAll(representationType.getContentProtection());
            } else {
                List<DescriptorType> currentCP = representationType.getContentProtection();
                if (contentProtection.size() == currentCP.size()) {
                    for (int i = 0; i < currentCP.size(); i++) {
                        DescriptorType a = currentCP.get(i);
                        DescriptorType b = contentProtection.get(i);
                        if (!(Objects.equals(a.getValue(), b.getValue()) && Objects.equals(a.getSchemeIdUri(), b.getSchemeIdUri()) && Objects.equals(a.getId(), b.getId()))) {
                            return;
                        }

                    }
                }
            }
        }
        if (!contentProtection.isEmpty()) {
            for (RepresentationBaseType representationType : parent.getRepresentation()) {
                representationType.getContentProtection().clear();
            }
            parent.getContentProtection().addAll(contentProtection);
        }

    }


}
