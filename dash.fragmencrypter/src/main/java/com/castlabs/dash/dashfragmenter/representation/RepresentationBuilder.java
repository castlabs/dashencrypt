package com.castlabs.dash.dashfragmenter.representation;


import mpegDashSchemaMpd2011.RepresentationType;

public interface RepresentationBuilder {
    RepresentationType getOnDemandRepresentation();
    RepresentationType getLiveRepresentation();
    String getSource();
}
