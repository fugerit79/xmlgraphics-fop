/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo.flow;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.properties.*;
import org.apache.fop.layout.*;
import org.apache.fop.datatypes.ColorType;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.area.Trait;
import org.apache.fop.area.Resolveable;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.Area;
import org.apache.fop.layoutmgr.InlineStackingLayoutManager;
import org.apache.fop.layoutmgr.LMiter;
import org.apache.fop.layoutmgr.LayoutManager;

// Java
import java.util.Enumeration;
import java.awt.Rectangle;
import java.util.List;
import java.util.ArrayList;

public class BasicLink extends Inline {
    String link = null;
    boolean external = false;

    public BasicLink(FONode parent) {
        super(parent);
    }

    // add start and end properties for the link
    public void addLayoutManager(List lms) {
        setup();
        lms.add(new InlineStackingLayoutManager(this,
                     new LMiter(children.listIterator())) {
                    protected InlineParent createArea() {
                        InlineParent area = super.createArea();
                        setupLinkArea(parentLM, area);
                        return area;
                    }
                });
    }

    protected void setupLinkArea(LayoutManager parentLM, InlineParent area) {
        if(link == null) {
            return;
        }
        if(external) {
            area.addTrait(Trait.EXTERNAL_LINK, link);
        } else {
            PageViewport page = parentLM.resolveRefID(link);
            if(page != null) {
                area.addTrait(Trait.INTERNAL_LINK, page);
            } else {
                LinkResolver res = new LinkResolver(link, area);
                parentLM.addUnresolvedArea(link, res);
            }
        }
    }

    public void setup() {
        String destination;
        int linkType;

        // Common Accessibility Properties
        AccessibilityProps mAccProps = propMgr.getAccessibilityProps();

        // Common Aural Properties
        AuralProps mAurProps = propMgr.getAuralProps();

        // Common Border, Padding, and Background Properties
        BorderAndPadding bap = propMgr.getBorderAndPadding();
        BackgroundProps bProps = propMgr.getBackgroundProps();

        // Common Margin Properties-Inline
        MarginInlineProps mProps = propMgr.getMarginInlineProps();

        // Common Relative Position Properties
        RelativePositionProps mRelProps = propMgr.getRelativePositionProps();

        // this.properties.get("alignment-adjust");
        // this.properties.get("alignment-baseline");
        // this.properties.get("baseline-shift");
        // this.properties.get("destination-place-offset");
        // this.properties.get("dominant-baseline");
        String ext =  properties.get("external-destination").getString();
        setupID();
        // this.properties.get("indicate-destination");
        String internal = properties.get("internal-destination").getString();
        if(ext.length() > 0) {
            link = ext;
            external = true;
        } else if(internal.length() > 0) {
            link = internal;
        } else {
            getLogger().error("basic-link requires an internal or external destination");
        }
        // this.properties.get("keep-together");
        // this.properties.get("keep-with-next");
        // this.properties.get("keep-with-previous");
        // this.properties.get("line-height");
        // this.properties.get("line-height-shift-adjustment");
        // this.properties.get("show-destination");
        // this.properties.get("target-processing-context");
        // this.properties.get("target-presentation-context");
        // this.properties.get("target-stylesheet");

    }

    protected static class LinkResolver implements Resolveable {
        private boolean resolved = false;
        private String idRef;
        // NOTE: there will be a problem with serialization
        private Area area;

        public LinkResolver(String id, Area a) {
            idRef = id;
            area = a;
        }

        public boolean isResolved() {
            return resolved;
        }

        public String[] getIDs() {
            return new String[] {idRef};
        }

        /**
         * Resolve by removing the id link and replacing with
         * an internal link.
         */
        public void resolve(String id, List pages) {
            if(idRef.equals(id) && pages != null) {
                PageViewport page = (PageViewport)pages.get(0);
                area.addTrait(Trait.INTERNAL_LINK, page);
            }
        }

    }

}

