package com.castlabs.dash.dashfragmenter.representation;

import mpegDashSchemaMpd2011.*;
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
        for (PeriodType periodType : mpdDocument.getMPD().getPeriodArray()) {
            for (AdaptationSetType adaptationSetType : periodType.getAdaptationSetArray()) {
                optimize(adaptationSetType);
            }
        }
    }

    public static void optimize(AdaptationSetType adaptationSetType) {
        optimizeContentProtection(adaptationSetType, adaptationSetType.getRepresentationArray());
        optimizeAttribute(adaptationSetType, adaptationSetType.getRepresentationArray(), "mimeType");
        optimizeAttribute(adaptationSetType, adaptationSetType.getRepresentationArray(), "codecs");
        optimizeAttribute(adaptationSetType, adaptationSetType.getRepresentationArray(), "profiles");
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
