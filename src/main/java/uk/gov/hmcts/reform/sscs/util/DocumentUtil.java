package uk.gov.hmcts.reform.sscs.util;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.equalsAnyIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoUploadParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;

public class DocumentUtil {

    private DocumentUtil() {
        //
    }

    public static boolean isFileAMedia(DocumentLink docLink) {
        return docLink != null
                && isNotBlank(docLink.getDocumentUrl())
                && equalsAnyIgnoreCase(getExtension(docLink.getDocumentFilename()), "mp3", "mp4");
    }

    public static boolean isFileAPdf(DocumentLink docLink) {
        return docLink != null
                && isNotBlank(docLink.getDocumentUrl())
                && equalsAnyIgnoreCase("pdf", getExtension(docLink.getDocumentFilename()));
    }

    public static String userFriendlyName(String documentType) {
        return StringUtils.capitalize(StringUtils.join(Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(documentType)).map(StringUtils::uncapitalize).toArray(String[]::new), " "));
    }

    public static AudioVideoUploadParty getUploader(List<String> roles){
        return Arrays.stream(AudioVideoUploadParty.values()).sequential().filter(i -> roles.contains(i.getValue())).findFirst().orElse(null);
    }
}
