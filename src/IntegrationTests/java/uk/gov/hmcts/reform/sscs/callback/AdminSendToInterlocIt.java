package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;




@SpringBootTest
@AutoConfigureMockMvc
public class AdminSendToInterlocIt extends AbstractEventIt {

    @Before
    public void setup() throws IOException {
        setup("callback/adminSendToInterloc.json");
    }

    @Test
    public void callToAboutToStart_willPopulateWhoReviewsCase() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        DynamicList expected = new DynamicList(
                new DynamicListItem("", ""),
                Arrays.asList(new DynamicListItem("reviewByTcw", "Review by TCW"),
                        new DynamicListItem("reviewByJudge", "Review by Judge"))
        );
        assertEquals(expected, result.getData().getSelectWhoReviewsCase());
    }

    @Test
    public void callToAboutToSubmit_willPopulateWhoReviewsCase() throws Exception {
        String json = getJson("callback/adminSendToInterlocSelectedWhoToReviewCase.json");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertNull(result.getData().getSelectWhoReviewsCase());
        assertEquals("reviewByTcw", result.getData().getInterlocReviewState());
    }


}


