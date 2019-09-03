package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class DwpActionWithdrawalHandlerTest extends AdminAppealWithdrawnBase {

    private final DwpActionWithdrawalHandler handler = new DwpActionWithdrawalHandler();

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,DWP_ACTION_WITHDRAWAL,withdrawalReceived,true",
        "ABOUT_TO_SUBMIT,DWP_ACTION_WITHDRAWAL,anyOtherValue,false",
        "ABOUT_TO_SUBMIT,DWP_ACTION_WITHDRAWAL,null,false",
        "ABOUT_TO_START,DWP_ACTION_WITHDRAWAL,withdrawalReceived,false",
        "SUBMITTED,DWP_ACTION_WITHDRAWAL,withdrawalReceived,false",
        "MID_EVENT,ADMIN_APPEAL_WITHDRAWN,withdrawalReceived,false",
        "ABOUT_TO_SUBMIT,ISSUE_FURTHER_EVIDENCE,withdrawalReceived,false",
        "null,DWP_ACTION_WITHDRAWAL,withdrawalReceived,false",
        "ABOUT_TO_SUBMIT,null,withdrawalReceived,false",
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType,
                          @Nullable String dwpStateValue, boolean expectedResult) throws IOException {
        boolean actualResult = handler.canHandle(callbackType, buildTestCallback(eventType,
            "dwpActionWithdrawalCallback.json", dwpStateValue));
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void handle() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualResult = handler.handle(CallbackType.ABOUT_TO_SUBMIT,
            buildTestCallback(EventType.DWP_ACTION_WITHDRAWAL, "dwpActionWithdrawalCallback.json"));

        String expectedCaseData = fetchData("callback/withdrawnappeals/dwpActionWithdrawalExpectedCaseData.json");
        assertEquals("Withdrawn", actualResult.getData().getDwpState());
        assertThatJson(actualResult.getData()).isEqualTo(expectedCaseData);
    }
}