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

package org.apache.fop.intermediate;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.custommonkey.xmlunit.XMLTestCase;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.events.model.EventSeverity;
import org.apache.fop.layoutengine.TestEnvironment;
import org.apache.fop.util.ConsoleEventListenerForTests;

/**
 * Abstract base class for intermediate format tests.
 */
public abstract class AbstractIntermediateTestCase extends XMLTestCase {

    /** the test environment */
    protected static TestEnvironment env = new TestEnvironment();

    /** the FOP factory */
    protected FopFactory fopFactory;

    /** the main base directory for tests */
    protected File mainDir = new File("test/layoutengine");
    /** the directory containing the tests */
    protected File testDir = new File(mainDir, "standard-testcases");
    /** the output directory for any files generated by the tests */
    protected File outputDir;

    /** the test file */
    protected File testFile;
    /** the test document as DOM */
    protected Document testDoc;
    /** the intermediate format document as DOM */
    protected Document intermediate;

    /**
     * Constructor for the test suite that is used for each test file.
     * @param testFile the test file to run
     * @throws IOException if an I/O error occurs while loading the test case
     */
    public AbstractIntermediateTestCase(File testFile)
            throws IOException {
        super(testFile.getName());
        this.testFile = testFile;
    }

    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();
        setupOutputDirectory();
        this.testDoc = env.loadTestCase(testFile);
        this.fopFactory = env.getFopFactory(testDoc);
        intermediate = buildIntermediateDocument(env.getTestcase2FOStylesheet());
        if (outputDir != null) {
            env.saveDOM(intermediate, new File(outputDir,
                    getName() + ".1" + getIntermediateFileExtension()));
        }
    }

    /** {@inheritDoc} */
    protected void tearDown() throws Exception {
        //Release memory
        this.intermediate = null;
        this.fopFactory = null;
        this.testDoc = null;
        super.tearDown();
    }

    /**
     * Returns the file extension for the intermediate file format.
     * @return the file extension
     */
    protected abstract String getIntermediateFileExtension();

    /**
     * Returns the MIME type for which to test or to mimic for the intermediate format.
     * @return the MIME type
     */
    protected String getTargetMIME() {
        return MimeConstants.MIME_PDF;
    }

    /**
     * Validates the intermediate format file.
     * @param doc the intermediate file
     * @throws IOException if an IO error occurs while loading the schema
     * @throws SAXException if a SAX-related exception (including a validation error) occurs
     */
    protected void validate(Document doc) throws SAXException, IOException {
        //nop by default
    }

    /**
     * Builds an intermediate format document from a source file.
     * @param templates the (optional) stylesheet
     * @return the intermediate format document as a DOM
     * @throws Exception if an error occurs while processing the document
     */
    protected abstract Document buildIntermediateDocument(Templates templates) throws Exception;

    /**
     * Creates a new FOP user agent.
     * @return the user agent
     */
    protected FOUserAgent createUserAgent() {
        FOUserAgent userAgent = fopFactory.newFOUserAgent();
        try {
            userAgent.setBaseURL(testDir.toURI().toURL().toExternalForm());
            userAgent.getEventBroadcaster().addEventListener(
                    new ConsoleEventListenerForTests(testFile.getName(), EventSeverity.FATAL));
        } catch (MalformedURLException e) {
            //ignore, won't happen
        }
        return userAgent;
    }

    /**
     * Sets up the output directory.
     */
    protected void setupOutputDirectory() {
        String s = System.getProperty("fop.intermediate.outdir");
        if (s != null && s.length() > 0) {
            outputDir = new File(s);
            outputDir.mkdirs();
        }
    }

    /**
     * Tests the area tree parser by running the parsed area tree again through the area tree
     * renderer. The source and result documents are compared to each other.
     * @throws Exception if the test fails
     */
    public void testParserToIntermediateFormat() throws Exception {
        validate(intermediate);
        Source src = new DOMSource(intermediate);
        Document doc = parseAndRenderToIntermediateFormat(src);
        if (outputDir != null) {
            File tgtFile = new File(outputDir, getName() + ".2" + getIntermediateFileExtension());
            env.saveDOM(doc, tgtFile);
        }

        assertXMLEqual(intermediate, doc);
    }

    /**
     * Parses the intermediate file and renders it back to the intermediate format.
     * @param src the source for the intermediate file
     * @return a DOM Document with the re-created intermediate file
     * @throws Exception if an error occurs while processing the document
     */
    protected abstract Document parseAndRenderToIntermediateFormat(Source src) throws Exception;

    /**
     * Tests the area tree parser by sending the parsed area tree to the PDF Renderer. Some
     * errors might be caught by the PDFRenderer.
     * @throws Exception if the test fails
     */
    public void testParserToPDF() throws Exception {
        OutputStream out;
        if (outputDir != null) {
            File tgtFile = new File(outputDir, getName() + ".pdf");
            out = new FileOutputStream(tgtFile);
            out = new BufferedOutputStream(out);
        } else {
            out = new NullOutputStream();
        }
        try {
            Source src = new DOMSource(intermediate);
            parseAndRender(src, out);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Parses and renders an intermediate format document to a final format.
     * @param src the source document
     * @param out the target output stream
     * @throws Exception if an error occurs while rendering the document
     */
    protected abstract void parseAndRender(Source src, OutputStream out)
            throws Exception;

    /**
     * Sets an error listener which doesn't swallow errors like Xalan's default one.
     * @param transformer the transformer to set the error listener on
     */
    protected void setErrorListener(Transformer transformer) {
        transformer.setErrorListener(new ErrorListener() {

            public void error(TransformerException exception) throws TransformerException {
                throw exception;
            }

            public void fatalError(TransformerException exception) throws TransformerException {
                throw exception;
            }

            public void warning(TransformerException exception) throws TransformerException {
                //ignore
            }

        });
    }

}
