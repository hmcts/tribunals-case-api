package uk.gov.hmcts.reform.sscs.service.converter;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;

public class ConvertSscsCaseDataIntoSessionDraftTest {
    @Test(expected = NullPointerException.class)
    public void attemptToConvertNull() {
        new ConvertSscsCaseDataIntoSessionDraft().convert(null);
    }

    @Test(expected = NullPointerException.class)
    public void attemptToConvertNullAppeal() {
        SscsCaseData caseData = SscsCaseData.builder().build();
        new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
    }

    @Test
    public void convertPopulatedCaseData() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .title("Mrs.")
                        .firstName("Ap")
                        .lastName("Pellant")
                        .build()
                    )
                    .identity(Identity.builder()
                        .dob("1998-12-31")
                        .nino("SC 94 27 06 A")
                        .build()
                    )
                    .address(Address.builder()
                        .line1("1 Appellant Close")
                        .town("Appellant-town")
                        .county("Appellant-county")
                        .postcode("TS1 1ST")
                        .build()
                    )
                    .contact(Contact.builder()
                        .mobile("07911123456")
                        .email("appellant@gmail.com")
                        .build()
                    )
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .build()
            )
            .build();

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertEquals("Personal Independence Payment (PIP)", actual.getBenefitType().getBenefitType());
        assertEquals("yes", actual.getCreateAccount().getCreateAccount());
        assertEquals("yes", actual.getHaveAMrn().getHaveAMrn());
        assertEquals("1", actual.getMrnDate().getMrnDateDetails().getDay());
        assertEquals("2", actual.getMrnDate().getMrnDateDetails().getMonth());
        assertEquals("2010", actual.getMrnDate().getMrnDateDetails().getYear());
        assertEquals("yes", actual.getCheckMrn().getCheckedMrn());
        assertEquals("Forgot to send it", actual.getMrnOverThirteenMonthsLate().getReasonForBeingLate());
        assertEquals("1", actual.getDwpIssuingOffice().getPipNumber());
        assertEquals("no", actual.getAppointee().getIsAppointee());
        assertEquals("Mrs.", actual.getAppellantName().getTitle());
        assertEquals("Ap", actual.getAppellantName().getFirstName());
        assertEquals("Pellant", actual.getAppellantName().getLastName());
        assertEquals("31", actual.getAppellantDob().getDate().getDay());
        assertEquals("12", actual.getAppellantDob().getDate().getMonth());
        assertEquals("1998", actual.getAppellantDob().getDate().getYear());
        assertEquals("SC 94 27 06 A", actual.getAppellantNino().getNino());
        assertEquals("1 Appellant Close", actual.getAppellantContactDetails().getAddressLine1());
        assertEquals(null, actual.getAppellantContactDetails().getAddressLine2());
        assertEquals("Appellant-town", actual.getAppellantContactDetails().getTownCity());
        assertEquals("Appellant-county", actual.getAppellantContactDetails().getCounty());
        assertEquals("TS1 1ST", actual.getAppellantContactDetails().getPostCode());
        assertEquals("07911123456", actual.getAppellantContactDetails().getPhoneNumber());
        assertEquals("appellant@gmail.com", actual.getAppellantContactDetails().getEmailAddress());
    }

    @Test
    public void convertPopulatedCaseDataWithAppointee() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .address(Address.builder()
                        .postcode("AP1 14NT")
                        .build()
                    )
                    .appointee(Appointee.builder()
                        .name(Name.builder().firstName("Ap").lastName("Pointee").build())
                        .build()
                    )
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .build()
            )
            .build();

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertEquals("yes", actual.getAppointee().getIsAppointee());
    }

    @Test
    public void convertPopulatedCaseDataWithoutMrnDate() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .address(Address.builder()
                        .postcode("AP1 14NT")
                        .build()
                    )
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .build()
                )
                .build()
            )
            .build();

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertEquals("no", actual.getHaveAMrn().getHaveAMrn());
        assertNull(actual.getMrnDate());
        assertEquals("no", actual.getCheckMrn().getCheckedMrn());
        assertNull(actual.getMrnOverThirteenMonthsLate());
    }
}
