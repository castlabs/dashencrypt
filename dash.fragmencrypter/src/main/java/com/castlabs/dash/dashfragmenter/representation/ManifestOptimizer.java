package com.castlabs.dash.dashfragmenter.representation;

import mpegDashSchemaMpd2011.*;
import org.apache.commons.lang.math.Fraction;
import org.apache.xmlbeans.GDuration;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pushes common attributes and elements down to the parent element.
 */
public class ManifestOptimizer {
    public static void optimize(MPDDocument mpdDocument) {
        adjustDuration(mpdDocument);
        for (PeriodType periodType : mpdDocument.getMPD().getPeriodArray()) {
            for (AdaptationSetType adaptationSetType : periodType.getAdaptationSetArray()) {
                optimize(adaptationSetType);
                adjustMinMax(adaptationSetType, "width");
                adjustMinMax(adaptationSetType, "height");
                adjustMinMax(adaptationSetType, "bandwidth");
                adjustMinMaxFrameRate(adaptationSetType); // special handling cause the type is special (fraction)
            }
        }
    }

    public static void adjustDuration(MPDDocument mpdDocument) {
        GDuration total = new GDuration();
        for (PeriodType periodType : mpdDocument.getMPD().getPeriodArray()) {
            total = total.add(periodType.getDuration());
        }
        mpdDocument.getMPD().setMediaPresentationDuration(total);
    }

    public static void adjustMinMaxFrameRate(AdaptationSetType adaptationSetType) {
        RepresentationType representationArray[] = adaptationSetType.getRepresentationArray();
        Fraction min = null, max = null;
        for (RepresentationType representationType : representationArray) {
            Node attr = representationType.getDomNode().getAttributes().getNamedItem("frameRate");
            if (attr != null) {
                Fraction f = Fraction.getFraction(attr.getNodeValue());

                min = min == null || f.compareTo(min) < 0 ? f : min;
                max = max == null || f.compareTo(max) > 0 ? f : max;
            }
        }
        if (max != null && !min.equals(max)) { // min/max doesn't make sense when both values are the same
            Node adaptationSet = adaptationSetType.getDomNode();
            Node minAttr =  adaptationSet.getOwnerDocument().createAttribute("minFrameRate");
            minAttr.setNodeValue(min.toString());
            adaptationSet.getAttributes().setNamedItem(minAttr);
            Node maxAttr =  adaptationSet.getOwnerDocument().createAttribute("maxFrameRate");
            maxAttr.setNodeValue(max.toString());
            adaptationSet.getAttributes().setNamedItem(maxAttr);

        }
    }

    public static void adjustMinMax(AdaptationSetType adaptationSetType, String attrName) {
        RepresentationType representationArray[] = adaptationSetType.getRepresentationArray();
        long min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (RepresentationType representationType : representationArray) {
            Node attr = representationType.getDomNode().getAttributes().getNamedItem(attrName);
            if (attr != null) {
                int n = Integer.parseInt(attr.getNodeValue());
                min = Math.min(n, min);
                max = Math.max(n, max);
            }
        }
        if (min != Integer.MAX_VALUE && min != max) {
            Node adaptationSet = adaptationSetType.getDomNode();
            Node minAttr =  adaptationSet.getOwnerDocument().createAttribute("min" + attrName.substring(0, 1).toUpperCase() + attrName.substring(1));
            minAttr.setNodeValue("" + min);
            adaptationSet.getAttributes().setNamedItem(minAttr);
            Node maxAttr =  adaptationSet.getOwnerDocument().createAttribute("max" + attrName.substring(0, 1).toUpperCase() + attrName.substring(1));
            maxAttr.setNodeValue("" + max);
            adaptationSet.getAttributes().setNamedItem(maxAttr);

        }
    }

    public static void optimize(AdaptationSetType adaptationSetType) {
        optimizeContentProtection(adaptationSetType, adaptationSetType.getRepresentationArray());
        optimizeAttribute(adaptationSetType, adaptationSetType.getRepresentationArray(), "mimeType");
        // This is commented out as customer P0132's chromecast player doesn't look for the codecs attribute on AdaptationSetBuilder level
        // The deal is to update the player before 2016. If you see this here in 2016 you can remove the comment and optimize
        // the codecs attribute as well.
        // optimizeAttribute(adaptationSetType, adaptationSetType.getRepresentationArray(), "codecs");
        optimizeAttribute(adaptationSetType, adaptationSetType.getRepresentationArray(), "profiles");
        optimizeAttribute(adaptationSetType, adaptationSetType.getRepresentationArray(), "width");
        optimizeAttribute(adaptationSetType, adaptationSetType.getRepresentationArray(), "height");
        optimizeAttribute(adaptationSetType, adaptationSetType.getRepresentationArray(), "frameRate");
        optimizeAttribute(adaptationSetType, adaptationSetType.getRepresentationArray(), "sar");
    }

    private static void optimizeAttribute(AdaptationSetType adaptationSetType, RepresentationType[] representationArray, String attrName) {
        String value = null;
        for (RepresentationType representationType : representationArray) {
            Node attr = representationType.getDomNode().getAttributes().getNamedItem(attrName);
            if (attr == null) {
                return; // no need to move it around when it doesn't exist
            }
            String _value = attr.getNodeValue();
            if (value == null || value.equals(_value)) {
                value = _value;
            } else {
                return;
            }
        }
        Node as = adaptationSetType.getDomNode();
        Attr attr = as.getOwnerDocument().createAttribute(attrName);
        attr.setValue(value);
        as.getAttributes().setNamedItem(attr);
        for (RepresentationType representationType : representationArray) {
            representationType.getDomNode().getAttributes().removeNamedItem(attrName);
        }
    }

    public static void optimizeContentProtection(RepresentationBaseType parent, RepresentationBaseType[] children) {
        mpegDashSchemaMpd2011.DescriptorType[] contentProtection = new mpegDashSchemaMpd2011.DescriptorType[0];
        for (RepresentationBaseType representationType : children) {
            if (contentProtection.length == 0) {
                List<DescriptorType> cpa = new ArrayList<DescriptorType>();
                for (DescriptorType descriptorType : representationType.getContentProtectionArray()) {
                    cpa.add((DescriptorType) descriptorType.copy());
                }
                contentProtection = cpa.toArray(new DescriptorType[representationType.getContentProtectionArray().length]);
            } else {
                DescriptorType[] currentCP = representationType.getContentProtectionArray();
                if (contentProtection.length == currentCP.length) {
                    for (int i = 0; i < currentCP.length; i++) {
                        DescriptorType a = currentCP[i];
                        DescriptorType b = contentProtection[i];
                        if (!a.xmlText().equals(b.xmlText())) {
                            return;
                        }

                    }
                }
            }
        }
        if (contentProtection.length != 0) {
            for (RepresentationBaseType representationType : children) {
                representationType.setContentProtectionArray(new DescriptorType[0]);
            }
            parent.setContentProtectionArray(contentProtection);
        }

    }


}
