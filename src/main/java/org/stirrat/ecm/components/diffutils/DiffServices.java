package org.stirrat.ecm.components.diffutils;

import intradoc.common.ServiceException;
import intradoc.data.DataBinder;
import intradoc.data.DataException;
import intradoc.data.DataResultSet;
import intradoc.filestore.FileStoreProvider;
import intradoc.provider.Provider;
import intradoc.provider.Providers;
import intradoc.provider.ServerRequestUtils;
import intradoc.server.DirectoryLocator;
import intradoc.server.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.ucmtwine.annotation.Binder;
import org.ucmtwine.annotation.ServiceMethod;

import difflib.DiffUtils;
import difflib.Patch;

public class DiffServices {

  private static final int DEFAULT_CONTEXT_LINE_COUNT = 3;

  /**
   * Diff two different revisions of the same content item.
   * 
   * @param dDocName
   * @param id1
   * @param id2
   * @throws DataException
   * @throws ServiceException
   */
  @ServiceMethod(name = "DIFF_REVISIONS", template = "TPL_DIFF_RESULT")
  public void diffRevisions(@Binder(name = "id1") long id1, @Binder(name = "id2") long id2, Service service)
      throws DataException, ServiceException {
    String file1 = getFileNameFromRevision(id1, service);
    String file2 = getFileNameFromRevision(id2, service);

    List<String> uDiff = diffFiles(file1, file2, service.getBinder());

    putDiffInBinder(uDiff, service.getBinder());
  }

  /**
   * Diff a revision and the latest copy from a provider.
   * 
   * @param dID
   * @param provider
   * @return
   * @throws DataException
   * @throws ServiceException
   */
  @ServiceMethod(name = "DIFF_EXTERNAL", template = "TPL_DIFF_RESULT")
  public void diffExternal(@Binder(name = "dID") long dID, @Binder(name = "provider") String providerName,
      Service service) throws ServiceException, DataException {

    Provider provider = Providers.getProvider(providerName);

    if (provider == null || provider.isInError()) {
      throw new DataException("Provider '" + providerName + "' is not found or in error");
    }

    if (!provider.isProviderOfType("outgoing")) {
      throw new ServiceException("Provider '" + provider.getName() + "' must be an outgoing provider");
    }

    String localFile = getFileNameFromRevision(dID, service);

    DataBinder requestBinder = new DataBinder();
    DataBinder responseBinder = new DataBinder();

    requestBinder.putLocal("IdcService", "GET_FILE");
    requestBinder.putLocal("dDocName", service.getBinder().getLocal("dDocName"));
    requestBinder.putLocal("RevisionSelectionMethod", "Latest");
    ServerRequestUtils.doAdminProxyRequest(provider, requestBinder, responseBinder, service);

    if (responseBinder.getLocal("downloadFile:path") == null) {
      throw new ServiceException("Unable to determine latest revision of item using outgoing provider '"
          + provider.getName());
    }

    String remoteFile = responseBinder.getLocal("downloadFile:path");

    if (remoteFile == null) {
      return;
    }

    List<String> uDiff = diffFiles(remoteFile, localFile, service.getBinder());

    putDiffInBinder(uDiff, service.getBinder());
  }

  /**
   * Diff each item returned in a search query with the same items found via an
   * outgoing provider.
   * 
   * @param QueryText
   * @param provider
   * @return
   * @throws DataException
   * @throws ServiceException
   */
  @ServiceMethod(name = "DIFF_SEARCH_RESULTS", template = "TPL_DIFF_RESULT")
  public void diffSearchResults(@Binder(name = "provider") String providerName, Service service)
      throws ServiceException, DataException {

    Provider provider = Providers.getProvider(providerName);

    if (provider == null || provider.isInError()) {
      throw new DataException("Provider '" + providerName + "' is not found or in error");
    }

    if (!provider.isProviderOfType("outgoing")) {
      throw new ServiceException("Provider '" + provider.getName() + "' must be an outgoing provider");
    }

    service.executeSafeServiceInNewContext("GET_SEARCH_RESULTS", false);

    DataResultSet rs = (DataResultSet) service.getBinder().getResultSet("SearchResults");

    if (rs == null) {
      throw new ServiceException("No SearchResults result set.");
    }

    List<String> allDiffs = new LinkedList<String>();

    if (rs.getNumRows() > 0 && rs.first()) {

      do {
        long dID = Long.parseLong(rs.getStringValueByName("dID"));
        String dDocName = rs.getStringValueByName("dDocName");

        String localFile = getFileNameFromRevision(dID, service);

        DataBinder requestBinder = new DataBinder();
        DataBinder responseBinder = new DataBinder();

        requestBinder.putLocal("IdcService", "GET_FILE");
        requestBinder.putLocal("dDocName", dDocName);
        requestBinder.putLocal("RevisionSelectionMethod", "Latest");
        ServerRequestUtils.doAdminProxyRequest(provider, requestBinder, responseBinder, service);

        if (responseBinder.getLocal("downloadFile:path") == null) {
          throw new ServiceException("Unable to determine latest revision of '" + dDocName
              + "' using outgoing provider '" + provider.getName());
        }

        String remoteFile = responseBinder.getLocal("downloadFile:path");

        if (remoteFile == null) {
          allDiffs.add("--- not found");
          allDiffs.add("+++ " + localFile);
        } else {

          List<String> diff = diffFiles(remoteFile, localFile, service.getBinder());

          if (diff.size() > 0) {
            allDiffs.addAll(diff);

          } else {
            allDiffs.add("--- " + remoteFile);
            allDiffs.add("+++ " + localFile);
            allDiffs.add("@@ no changes @@");
          }

        }

        allDiffs.add(" "); // empty line for spacing between diffs

      } while (rs.next());
    }

    putDiffInBinder(allDiffs, service.getBinder());
  }

  /**
   * Diff two files and return the line by line diffs.
   * 
   * @param file1
   * @param file2
   * @param binder
   * @return
   */
  private List<String> diffFiles(String file1, String file2, DataBinder binder) {
    List<String> file1Content = fileToLines(file1);
    List<String> file2Content = fileToLines(file2);

    Patch patch = DiffUtils.diff(file1Content, file2Content);

    return DiffUtils.generateUnifiedDiff(file1, file2, file1Content, patch, DEFAULT_CONTEXT_LINE_COUNT);
  }

  private StringBuilder putDiffInBinder(List<String> uDiff, DataBinder binder) {
    StringBuilder diffOutput = new StringBuilder();
    StringBuilder diffHtml = new StringBuilder();

    for (String line : uDiff) {
      diffOutput.append(line).append("\n");

      switch (line.charAt(0)) {
      case '-':
        diffHtml.append("<span class=\"removed\">").append(StringEscapeUtils.escapeHtml(line)).append("</span>\n");
        break;

      case '+':
        diffHtml.append("<span class=\"added\">").append(StringEscapeUtils.escapeHtml(line)).append("</span>\n");
        break;

      case '@':
        diffHtml.append("<span class=\"special\">").append(StringEscapeUtils.escapeHtml(line)).append("</span>\n");
        break;

      default:
        diffHtml.append(StringEscapeUtils.escapeHtml(line)).append("\n");
        break;
      }
    }

    binder.putLocal("diffOutput", diffOutput.toString());
    binder.putLocal("diffHtml", diffHtml.toString());
    return diffOutput;
  }

  /**
   * Gets the file path to the vault given a dID
   * 
   * @param revisionId
   * @param service
   * @return
   * @throws ServiceException
   * @throws DataException
   */
  private String getFileNameFromRevision(long revisionId, Service service) throws ServiceException, DataException {
    service.getBinder().putLocal("dID", String.valueOf(revisionId));

    service.executeSafeServiceInNewContext("DOC_INFO", false);

    DataResultSet docInfo = (DataResultSet) service.getBinder().getResultSet("DOC_INFO");

    String fileName = docInfo.getStringValueByName("dDocName").toLowerCase();
    if (!docInfo.getStringValueByName("dReleaseState").equals("Y")) {
      fileName += "~" + docInfo.getStringValueByName("dRevLabel");
    }
    fileName += "." + docInfo.getStringValueByName("dWebExtension");
    String vaultFile = DirectoryLocator.computeDirectory(service.getBinder(), FileStoreProvider.R_WEB) + fileName;
    service.getBinder().removeResultSet("DOC_INFO");
    service.getBinder().removeResultSet("REVISION_HISTORY");
    return vaultFile;
  }

  /**
   * Read a file into a list of lines.
   * 
   * @param fileName
   * @return
   */
  private List<String> fileToLines(String fileName) {
    List<String> lines = new LinkedList<String>();
    String line = "";
    try {
      BufferedReader in = new BufferedReader(new FileReader(fileName));
      while ((line = in.readLine()) != null) {
        lines.add(line);
      }
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return lines;
  }
}
