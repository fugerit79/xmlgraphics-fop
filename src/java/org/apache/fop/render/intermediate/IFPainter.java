/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.render.intermediate;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import javax.xml.transform.Result;

import org.apache.fop.apps.FOUserAgent;

/**
 * Interface used to paint whole documents layouted by Apache FOP.
 * <p>
 * Call sequence:
 * <p>
 * <pre>
 * startDocument()
 *   startDocumentHeader()
 *   [handleExtension()]*
 *   endDocumentHeader()
 *   [
 *   startPageSequence()
 *     [
 *     startPage()
 *       startPageHeader()
 *         [handleExtension()]*
 *       endPageHeader()
 *       startPageContent()
 *         (#pageContent)+
 *       endPageContent()
 *       startPageTrailer()
 *         (addTarget())*
 *       endPageTrailer()
 *     endPage()
 *     ]*
 *   endPageSequence()
 *   ]*
 * endDocument()
 *
 * #box:
 * startBox()
 * (#pageContent)+
 * endBox()
 *
 * #pageContent:
 * (
 *   setFont() |
 *   drawText() |
 *   drawRect() |
 *   drawImage() |
 *   TODO etc. etc. |
 *   handleExtensionObject()
 * )
 * </pre>
 */
public interface IFPainter {

    /**
     * Set the user agent.
     * @param userAgent  The user agent
     */
    void setUserAgent(FOUserAgent userAgent);

    /**
     * Sets the JAXP Result object to receive the generated content.
     * @param result the JAXP Result object to receive the generated content
     * @throws IFException if an error occurs setting up the output
     */
    void setResult(Result result) throws IFException;

    /**
     * Indicates whether the painter supports to handle the pages in mixed order rather than
     * ascending order.
     * @return true if out-of-order handling is supported
     */
    boolean supportsPagesOutOfOrder();

    /**
     * Indicates the start of a document. This method may only be called once before any other
     * event method.
     * @throws IFException if an error occurs while handling this event
     */
    void startDocument() throws IFException;

    /**
     * Indicates the end of a document. This method may only be called once after the whole
     * document has been handled. Implementations can release resources (close streams). It is
     * an error to call any event method after this method.
     * @throws IFException if an error occurs while handling this event
     */
    void endDocument() throws IFException;

    /**
     * Indicates the start of the document header. This method is called right after the
     * {@code #startDocument()} method. Extensions sent to this painter between
     * {@code #startDocumentHeader()} and {@code #endDocumentHeader()} apply to the document as
     * a whole (like document metadata).
     * @throws IFException if an error occurs while handling this event
     */
    void startDocumentHeader() throws IFException;

    /**
     * Indicates the end of the document header. This method is called before the first
     * page sequence.
     * @throws IFException if an error occurs while handling this event
     */
    void endDocumentHeader() throws IFException;

    /**
     * Indicates the start of a new page sequence.
     * @param id the page sequence's identifier (or null if none is available)
     * @throws IFException if an error occurs while handling this event
     */
    void startPageSequence(String id) throws IFException;
    /**
     * Indicates the end of a page sequence.
     * @throws IFException if an error occurs while handling this event
     */
    void endPageSequence() throws IFException;

    /**
     * Indicates the start of a new page.
     * @param index the index of the page within the document (0-based)
     * @param name the page name (usually the formatted page number)
     * @param size the size of the page (equivalent to the MediaBox in PDF)
     * @throws IFException if an error occurs while handling this event
     */
    void startPage(int index, String name, Dimension size) throws IFException;

    /**
     * Indicates the end of a page
     * @throws IFException if an error occurs while handling this event
     */
    void endPage() throws IFException;

    /**
     * Indicates the start of the page header.
     * @throws IFException if an error occurs while handling this event
     */
    void startPageHeader() throws IFException;

    /**
     * Indicates the end of the page header.
     * @throws IFException if an error occurs while handling this event
     */
    void endPageHeader() throws IFException;

    /**
     * Indicates the start of the page content.
     * @throws IFException if an error occurs while handling this event
     */
    void startPageContent() throws IFException;

    /**
     * Indicates the end of the page content.
     * @throws IFException if an error occurs while handling this event
     */
    void endPageContent() throws IFException;

    /**
     * Indicates the start of the page trailer. The page trailer is used for writing down page
     * elements which are only know after handling the page itself (like PDF targets).
     * @throws IFException if an error occurs while handling this event
     */
    void startPageTrailer() throws IFException;

    /**
     * @todo Solve with extension because not all formats support that?
     */
    void addTarget(String name, int x, int y) throws IFException;

    /**
     * Indicates the end of the page trailer.
     * @throws IFException if an error occurs while handling this event
     */
    void endPageTrailer() throws IFException;

    void startBox(AffineTransform transform, Dimension size, boolean clip) throws IFException;
    void startBox(AffineTransform[] transforms, Dimension size, boolean clip) throws IFException;
    //For transform, Batik's org.apache.batik.parser.TransformListHandler/Parser can be used
    void endBox() throws IFException;

    /**
     * Updates the current font.
     * @param family the font family (or null if there's no change)
     * @param style the font style (or null if there's no change)
     * @param weight the font weight (or null if there's no change)
     * @param variant the font variant (or null if there's no change)
     * @param size the font size (or null if there's no change)
     * @param color the text color (or null if there's no change)
     * @throws IFException if an error occurs while handling this event
     */
    void setFont(String family, String style, Integer weight, String variant, Integer size,
            Color color) throws IFException;

    /**
     * Draws text. The initial coordinates (x and y) point to the starting point at the normal
     * baseline of the font. The arrays (dx and dy) are optional and can be used to achieve
     * effects like kerning.
     * @param x X-coordinate of the starting point of the text
     * @param y Y-coordinate of the starting point of the text
     * @param dx an array of adjustment values for each character in X-direction
     * @param dy an array of adjustment values for each character in Y-direction
     * @param text the text
     * @throws IFException if an error occurs while handling this event
     */
    void drawText(int x, int y, int[] dx, int[] dy, String text) throws IFException;

    /**
     * Draws a rectangle. Either fill or stroke has to be specified.
     * @param rect the rectangle's coordinates and extent
     * @param fill the fill paint (may be null)
     * @param stroke the stroke color (may be null)
     * @throws IFException if an error occurs while handling this event
     */
    void drawRect(Rectangle rect, Paint fill, Color stroke) throws IFException;
    void drawImage(String uri, Rectangle rect) throws IFException; //external images
    void startImage(Rectangle rect) throws IFException; //followed by a SAX stream (SVG etc.)
    void endImage() throws IFException;
    //etc. etc.

    /**
     * Handles an extension object. This can be a DOM document or any arbitrary
     * object. If an implementation doesn't know how to handle a particular extension it is simply
     * ignored.
     * @param extension the extension object
     * @throws IFException if an error occurs while handling this event
     */
    void handleExtensionObject(Object extension) throws IFException;

    //TODO Prototype the following:
    //ContentHandler handleExtension() throws Exception
}
