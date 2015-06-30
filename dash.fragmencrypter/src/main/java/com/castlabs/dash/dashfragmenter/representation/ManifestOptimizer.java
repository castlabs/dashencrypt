package com.castlabs.dash.dashfragmenter.representation;

import mpegDashSchemaMpd2011.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ManifestOptimizer {
    public void optimize(MPDDocument mpdDocument) {
        for (PeriodType periodType : mpdDocument.getMPD().getPeriodArray()) {
            for (AdaptationSetType adaptationSetType : periodType.getAdaptationSetArray()) {
                optimize(adaptationSetType);
            }
        }
    }

    public void optimize(AdaptationSetType adaptationSetType) {
        optimizeContentProtection(adaptationSetType, adaptationSetType.getRepresentationArray());
    }

    public void optimizeContentProtection(RepresentationBaseType parent, RepresentationBaseType[] children ) {
        mpegDashSchemaMpd2011.DescriptorType[] contentProtection = null;
        for (RepresentationBaseType representationType : children) {
            if (contentProtection == null) {
                List<DescriptorType> aaa = new ArrayList<DescriptorType>();
                for (DescriptorType descriptorType : representationType.getContentProtectionArray()) {
                    aaa.add((DescriptorType) descriptorType.copy());
                }
                contentProtection = aaa.toArray(new DescriptorType[representationType.getContentProtectionArray().length]);
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
        if (contentProtection != null) {
            for (RepresentationBaseType representationType : children) {
                representationType.setContentProtectionArray(new DescriptorType[0]);
            }
            parent.setContentProtectionArray(contentProtection);
        }

    }


}
