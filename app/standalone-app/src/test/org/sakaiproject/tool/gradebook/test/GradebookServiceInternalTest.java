/**********************************************************************************
*
* $Id$
*
***********************************************************************************
*
* Copyright (c) 2005, 2006 The Regents of the University of California, The MIT Corporation
*
* Licensed under the Educational Community License Version 1.0 (the "License");
* By obtaining, using and/or copying this Original Work, you agree that you have read,
* understand, and will comply with the terms and conditions of the Educational Community License.
* You may obtain a copy of the License at:
*
*      http://www.opensource.org/licenses/ecl1.php
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
* INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
* AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
* DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*
**********************************************************************************/
package org.sakaiproject.tool.gradebook.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.gradebook.GradebookDefinition;
import org.sakaiproject.component.gradebook.VersionedExternalizable;
import org.sakaiproject.section.api.coursemanagement.Course;
import org.sakaiproject.section.api.coursemanagement.CourseSection;
import org.sakaiproject.section.api.facade.Role;
import org.sakaiproject.service.gradebook.shared.AssessmentNotFoundException;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.AssignmentHasIllegalPointsException;
import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.service.gradebook.shared.GradingScaleDefinition;
import org.sakaiproject.tool.gradebook.GradeMapping;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.thoughtworks.xstream.converters.ConversionException;

/**
 * Test the service methods which interact with internally maintained data.
 * These methods have more complex authorization requirements.
 */
public class GradebookServiceInternalTest extends GradebookTestBase {

    private static final Log log = LogFactory.getLog(GradebookServiceInternalTest.class);

    private static final String GRADEBOOK_UID = "gradebookServiceTest";
    private static final String ASN_TITLE = "Assignment #1";
    private static final String EXT_ID_1 = "External #1";
    private static final String EXT_TITLE_1 = "External Title #1";
    private static final String INSTRUCTOR_UID = "Inst-1";
    private static final String TA_UID = "TA-1";
    private static final String SECTION_NAME = "Lab 01";
    private static final String STUDENT_IN_SECTION_UID = "StudentInLab";
    private static final String STUDENT_NOT_IN_SECTION_UID = "StudentNotInLab";
    private static final Double ASN_POINTS = new Double(40.0);
//    private static final String  = "";

    /**
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpInTransaction()
     */
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
 		gradebookFrameworkService.addGradebook(GRADEBOOK_UID, GRADEBOOK_UID);
 		Gradebook gradebook = gradebookManager.getGradebook(GRADEBOOK_UID);

        // Set up users, enrollments, teaching assignments, and sections.
        Course courseSite = integrationSupport.createCourse(GRADEBOOK_UID, GRADEBOOK_UID, false, false, false);
		addUsersEnrollments(gradebook, Arrays.asList(new String[] {STUDENT_IN_SECTION_UID, STUDENT_NOT_IN_SECTION_UID}));
		userManager.createUser(INSTRUCTOR_UID, null, null, null);
		integrationSupport.addSiteMembership(INSTRUCTOR_UID, GRADEBOOK_UID, Role.INSTRUCTOR);
		userManager.createUser(TA_UID, null, null, null);
		integrationSupport.addSiteMembership(TA_UID, GRADEBOOK_UID, Role.TA);
		List sectionCategories = sectionAwareness.getSectionCategories(GRADEBOOK_UID);
		CourseSection section = integrationSupport.createSection(courseSite.getUuid(), SECTION_NAME,
			(String)sectionCategories.get(0),
			new Integer(40), null, null, null, true, false, true,  false, false, false, false);
		integrationSupport.addSectionMembership(STUDENT_IN_SECTION_UID, section.getUuid(), Role.STUDENT);
		integrationSupport.addSectionMembership(TA_UID, section.getUuid(), Role.TA);

        // Add an internal assignment.
        gradebookManager.createAssignment(gradebook.getId(), ASN_TITLE, ASN_POINTS, new Date(), Boolean.FALSE,Boolean.FALSE);

        // Add an external assessment.
        gradebookExternalAssessmentService.addExternalAssessment(GRADEBOOK_UID, EXT_ID_1, null, EXT_TITLE_1, 10, null, "Samigo");
    }
    
    public void testGradebookMigration() throws Exception {    	
    	setAuthnId(INSTRUCTOR_UID);
 		Gradebook gradebook = gradebookManager.getGradebook(GRADEBOOK_UID);
    	
 		// Collect the default grade mappings for future reference.
    	GradeMapping defaultGradeMapping = gradebook.getSelectedGradeMapping();
    	GradeMapping nonDefaultGradeMapping = null;
    	for (GradeMapping gradeMapping : gradebook.getGradeMappings()) {
    		if (!gradeMapping.getName().equals(defaultGradeMapping.getName())) {
    			nonDefaultGradeMapping = gradeMapping;
    			break;
    		}
    	}
    	String firstGrade = nonDefaultGradeMapping.getGradingScale().getGrades().get(0);
    	double originalFirstGradeValue = nonDefaultGradeMapping.getGradeMap().get(firstGrade).doubleValue();
    	if (log.isDebugEnabled()) log.debug("nonDefaultGradeMapping=" + nonDefaultGradeMapping.getGradingScale().getUid() + ", firstGrade=" + firstGrade + ", value=" + originalFirstGradeValue);

    	gradebookManager.createAssignment(gradebook.getId(), "Duplicate", new Double(100), null, Boolean.TRUE, Boolean.TRUE);
    	gradebookManager.createAssignment(gradebook.getId(), "Released", new Double(50), null, Boolean.FALSE, Boolean.TRUE);
    	nonDefaultGradeMapping.getGradeMap().put(firstGrade, new Double(originalFirstGradeValue - 1.0));
    	gradebook.setSelectedGradeMapping(nonDefaultGradeMapping);
		gradebook.setAssignmentsDisplayed(false);	// Override the defaults
		gradebook.setCourseGradeDisplayed(true);
    	gradebookManager.updateGradebook(gradebook);
    	
    	String gradebookXml = gradebookService.getGradebookDefinitionXml(GRADEBOOK_UID);
    	GradebookDefinition gradebookDefinition = (GradebookDefinition)VersionedExternalizable.fromXml(gradebookXml);
    	if (log.isDebugEnabled()) log.debug("gradebookXml=" + gradebookXml);
    	
    	// Create the target gradebook.
    	String migrateToUid = "MigrateTo";
 		Gradebook newGradebook = getNewGradebook(migrateToUid);
		gradebookManager.createAssignment(newGradebook.getId(), "Duplicate", new Double(1.0), new Date(), Boolean.FALSE, Boolean.FALSE);

    	// Try to merge the old definition in.
 		gradebookService.mergeGradebookDefinitionXml(migrateToUid, gradebookXml);
 		newGradebook = gradebookManager.getGradebook(migrateToUid);
 		
 		// Make sure the old assignments were merged in.
 		List assignments = gradebookService.getAssignments(migrateToUid);
 		Map<String, Assignment>assignmentMap = new HashMap<String, Assignment>();
 		for (Object obj : assignments) {
 			Assignment assignment = (Assignment)obj;
 			assignmentMap.put(assignment.getName(), assignment);
 		}
 		Assert.assertTrue(assignmentMap.containsKey(ASN_TITLE));
    	
    	// All assignments should be unreleased even if they were released in the original.
		Assignment released = assignmentMap.get("Released");
		Assert.assertTrue(!released.isReleased());
		Assert.assertTrue(released.isCounted());
    	
    	// Externally managed assessments should not be included.
		Assert.assertTrue(!assignmentMap.containsKey(EXT_TITLE_1));
		
    	// Assignments with duplicate names shouldn't override existing assignments.
		Assignment duplicate = assignmentMap.get("Duplicate");
		Assert.assertTrue(duplicate.getPoints().doubleValue() == 1.0);
    	
    	// Student view options should stay as they were.
		Assert.assertTrue(newGradebook.isAssignmentsDisplayed());
		Assert.assertTrue(!newGradebook.isCourseGradeDisplayed());
    	
    	// Carry over the old gradebook's selected grading scheme if possible.
		GradeMapping migratedGradeMapping = newGradebook.getSelectedGradeMapping();
		Assert.assertTrue(migratedGradeMapping.getName().equals(nonDefaultGradeMapping.getName()));
		Assert.assertTrue(migratedGradeMapping.getGradeMap().get(firstGrade).doubleValue() != originalFirstGradeValue);
    	
    	// If the old grading scheme is not available to the new gradebook, leave
    	// the new gradebook's grading scheme alone.
		List newMappings = new ArrayList();
        GradingScaleDefinition def = new GradingScaleDefinition();
        def.setUid("BettingScale");
        def.setName("Just One Grading Scale");
        def.setGrades(Arrays.asList(new Object[] {"Win", "Draw", "Lose"}));
        def.setDefaultBottomPercents(Arrays.asList(new Object[] {new Double(80), new Double(40), new Double(0)}));
        newMappings.add(def);
        gradebookFrameworkService.setAvailableGradingScales(newMappings);
        getNewGradebook("BettingGradebook");
 		gradebookXml = gradebookService.getGradebookDefinitionXml("BettingGradebook");
 		gradebookService.mergeGradebookDefinitionXml(migrateToUid, gradebookXml);
 		newGradebook = gradebookManager.getGradebook(migrateToUid);
		Assert.assertTrue(newGradebook.getSelectedGradeMapping().getName().equals(nonDefaultGradeMapping.getName()));
    	
    	// Test the Externalizable feature.
    	ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    	ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
    	gradebookDefinition.writeExternal(objOut);
    	objOut.close();	// Required to close the XML string
    	if (log.isDebugEnabled()) log.debug("externalized gradebook=" + byteOut.toString("UTF-8"));
    	GradebookDefinition restoredGradebookDefinition = new GradebookDefinition();
    	restoredGradebookDefinition.readExternal(new ObjectInputStream(new ByteArrayInputStream(byteOut.toByteArray())));
    	if (log.isDebugEnabled()) log.debug("restored gradebook=" + restoredGradebookDefinition);
    }
    
    private Gradebook getNewGradebook(String gradebookUid) {
    	integrationSupport.createCourse(gradebookUid, gradebookUid, false, false, false);
    	gradebookFrameworkService.addGradebook(gradebookUid, gradebookUid);
		integrationSupport.addSiteMembership(INSTRUCTOR_UID, gradebookUid, Role.INSTRUCTOR);
 		return gradebookManager.getGradebook(gradebookUid);
    }

    public void testGradebookMigrationVersioning() throws Exception {
    	setAuthnId(INSTRUCTOR_UID);
    	String gradebookXml = gradebookService.getGradebookDefinitionXml(GRADEBOOK_UID);

    	DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    	Document document = documentBuilder.parse(new InputSource(new StringReader(gradebookXml)));
    	Element gradebookElement = document.getDocumentElement();
    	String versionXml = gradebookElement.getAttribute(VersionedExternalizable.VERSION_ATTRIBUTE);
    	Assert.assertTrue(versionXml.equals(GradebookDefinition.EXTERNALIZABLE_VERSION));
    	
    	// Mess with the converter's mind and make sure it's displeased.
    	gradebookElement.removeAttribute(VersionedExternalizable.VERSION_ATTRIBUTE);
    	gradebookElement.setAttribute(VersionedExternalizable.VERSION_ATTRIBUTE, "Who are you kidding?");
    	try {
    		String newXml = documentToString(document);
    		VersionedExternalizable.fromXml(newXml);
    		fail();
    	} catch (ConversionException e) {
    	}
    }

    public static String documentToString(Document doc) {
		String result = null;
    	try {
    		// Work around JDK 1.5 issue.
    		System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			transformer.transform(new DOMSource(doc.getDocumentElement()), new StreamResult(byteOut));
			if (byteOut != null) {
				result = byteOut.toString("UTF-8");
			}
		} catch (TransformerException e) {
			log.error(e);
		} catch (UnsupportedEncodingException e) {
			log.error(e);
		}
		return result;
    }
    
    public void testStudentRebuff() throws Exception {
    	setAuthnId(STUDENT_IN_SECTION_UID);
    	try {
			if (log.isInfoEnabled()) log.info("Ignore the upcoming authorization error...");
			gradebookService.getAssignments(GRADEBOOK_UID);
			fail();
		} catch (SecurityException e) {
		}
   }

	public void testExternalClientSupport() throws Exception {
		setAuthnId(TA_UID);

		List assignments = gradebookService.getAssignments(GRADEBOOK_UID);
		Assert.assertTrue(assignments.size() == 2);

		for (Iterator iter = assignments.iterator(); iter.hasNext(); ) {
			Assignment assignment = (Assignment)iter.next();

			if (assignment.isExternallyMaintained()) {
				Assert.assertTrue(EXT_TITLE_1.equals(assignment.getName()));
				// Make sure we can't update it.
				boolean gotSecurityException = false;
				try {
					if (log.isInfoEnabled()) log.info("Ignore the upcoming authorization error...");
					gradebookService.setAssignmentScore(GRADEBOOK_UID, EXT_TITLE_1, STUDENT_IN_SECTION_UID, new Double(9), "Service Test");
				} catch (SecurityException e) {
					gotSecurityException = true;
				}
				Assert.assertTrue(gotSecurityException);
			} else {
				Assert.assertTrue(ASN_TITLE.equals(assignment.getName()));
				Assert.assertTrue(assignment.getPoints().equals(ASN_POINTS));

				Assert.assertFalse(gradebookService.isUserAbleToGradeStudent(GRADEBOOK_UID, STUDENT_NOT_IN_SECTION_UID));
				boolean gotSecurityException = false;
				try {
					if (log.isInfoEnabled()) log.info("Ignore the upcoming authorization error...");
					gradebookService.getAssignmentScore(GRADEBOOK_UID, ASN_TITLE, STUDENT_NOT_IN_SECTION_UID);
				} catch (SecurityException e) {
					gotSecurityException = true;
				}
				gotSecurityException = false;
				try {
					if (log.isInfoEnabled()) log.info("Ignore the upcoming authorization error...");
					gradebookService.setAssignmentScore(GRADEBOOK_UID, ASN_TITLE, STUDENT_NOT_IN_SECTION_UID, new Double(39), "Service Test");
				} catch (SecurityException e) {
					gotSecurityException = true;
				}
				Assert.assertTrue(gotSecurityException);

				Assert.assertTrue(gradebookService.isUserAbleToGradeStudent(GRADEBOOK_UID, STUDENT_IN_SECTION_UID));
				Double score = gradebookService.getAssignmentScore(GRADEBOOK_UID, ASN_TITLE, STUDENT_IN_SECTION_UID);
				Assert.assertTrue(score == null);
				gradebookService.setAssignmentScore(GRADEBOOK_UID, ASN_TITLE, STUDENT_IN_SECTION_UID, new Double(39), "Service Test");
				score = gradebookService.getAssignmentScore(GRADEBOOK_UID, ASN_TITLE, STUDENT_IN_SECTION_UID);
				Assert.assertTrue(score.doubleValue() == 39.0);

				// Also test the case where there's a score already there.
				gradebookService.setAssignmentScore(GRADEBOOK_UID, ASN_TITLE, STUDENT_IN_SECTION_UID, new Double(37), "Different Service Test");
				score = gradebookService.getAssignmentScore(GRADEBOOK_UID, ASN_TITLE, STUDENT_IN_SECTION_UID);
				Assert.assertTrue(score.doubleValue() == 37.0);
			}
		}
	}

    public void testAddAssignment() throws Exception {    	
    	setAuthnId(INSTRUCTOR_UID);
 		
 		// Create an assignment definition.
 		String assignmentName = "Client-Created Quiz";
 		Assignment assignmentDefinition = new Assignment();
 		assignmentDefinition.setName(assignmentName);
 		assignmentDefinition.setPoints(new Double(50));
 		gradebookService.addAssignment(GRADEBOOK_UID, assignmentDefinition);
 		
 		// Make sure it's there and we can grade it.
		gradebookService.setAssignmentScore(GRADEBOOK_UID, assignmentName, STUDENT_IN_SECTION_UID, new Double(49), "Service Test");
		Double score = gradebookService.getAssignmentScore(GRADEBOOK_UID, assignmentName, STUDENT_IN_SECTION_UID);
		Assert.assertTrue(score.doubleValue() == 49.0);		
 		
 		// Make sure we can't add duplicate names.
 		assignmentDefinition.setPoints(new Double(40));
 		try {
 			gradebookService.addAssignment(GRADEBOOK_UID, assignmentDefinition);
 			fail();
 		} catch (ConflictingAssignmentNameException e) {}
		
 		// Make sure we don't accept zero-score assignments (at present).
 		assignmentDefinition.setName("Illegal Assignment");
 		assignmentDefinition.setPoints(new Double(0));
 		try {
 			gradebookService.addAssignment(GRADEBOOK_UID, assignmentDefinition);
 			fail();
 		} catch (AssignmentHasIllegalPointsException e) {}
    }
    
    public void testMoveExternalToInternal() throws Exception {
        // Add an external assessment score.
        gradebookExternalAssessmentService.updateExternalAssessmentScore(GRADEBOOK_UID, EXT_ID_1, STUDENT_IN_SECTION_UID, new Double(5));
        
        // Break the relationship off.
        gradebookExternalAssessmentService.setExternalAssessmentToGradebookAssignment(GRADEBOOK_UID, EXT_ID_1);
        
        // Make sure that the internal-access APIs work now.
    	setAuthnId(INSTRUCTOR_UID);
        Double score = gradebookService.getAssignmentScore(GRADEBOOK_UID, EXT_TITLE_1, STUDENT_IN_SECTION_UID);
        Assert.assertTrue(score.doubleValue() == 5.0);
        gradebookService.setAssignmentScore(GRADEBOOK_UID, EXT_TITLE_1, STUDENT_IN_SECTION_UID, new Double(10), "A Friend");
        score = gradebookService.getAssignmentScore(GRADEBOOK_UID, EXT_TITLE_1, STUDENT_IN_SECTION_UID);
        Assert.assertTrue(score.doubleValue() == 10.0);
        
        // Make sure that the external-management fields are nulled out.
        List assignments = gradebookService.getAssignments(GRADEBOOK_UID);
        org.sakaiproject.service.gradebook.shared.Assignment assignmentDefinition = null;
        for (Object obj : assignments) {
        	org.sakaiproject.service.gradebook.shared.Assignment asgn = (org.sakaiproject.service.gradebook.shared.Assignment)obj;
        	if (asgn.getName().equals(EXT_TITLE_1)) {
        		assignmentDefinition = asgn;
        		break;
        	}
        }
        Assert.assertTrue(!assignmentDefinition.isExternallyMaintained());
        Assert.assertTrue(assignmentDefinition.getExternalAppName() == null);
        
        // Make sure that the external-management APIs don't work any more.
        try {
        	gradebookExternalAssessmentService.updateExternalAssessmentScore(GRADEBOOK_UID, EXT_ID_1, STUDENT_IN_SECTION_UID, new Double(5));
        	fail();
        } catch (AssessmentNotFoundException e) {
        }
    }

}
