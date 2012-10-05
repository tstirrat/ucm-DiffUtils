package org.stirrat.ecm.components.diffutils;

import intradoc.common.ServiceException;
import intradoc.common.SystemUtils;
import intradoc.data.DataBinder;
import intradoc.data.DataException;
import intradoc.filestore.FileStoreProvider;
import intradoc.provider.Provider;
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
  public String diffRevisions(@Binder(name = "id1") long id1, @Binder(name = "id2") long id2, Service service)
      throws DataException, ServiceException {
    SystemUtils.trace("diffutils", "Diffing revisions left: " + String.valueOf(id1) + " right:" + String.valueOf(id2));

    String file1 = getFileNameFromRevision(id1, service);
    String file2 = getFileNameFromRevision(id2, service);

    return diffFiles(file1, file2, service.getBinder());
  }

  /**
   * Diff a revision and the latest copy from a provider.
   * 
   * @param dID
   * @param providerName
   * @return
   * @throws DataException
   * @throws ServiceException
   */
  @ServiceMethod(name = "DIFF_PROVIDER", template = "TPL_DIFF_RESULT")
  public String diffExternal(@Binder(name = "dID") long dID, @Binder(name = "providerName") Provider provider,
      Service service) throws ServiceException, DataException {

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
      return remoteFile;
    }

    return diffFiles(remoteFile, localFile, service.getBinder());
  }

  /**
   * Diff two files and output the result into diffOutput and diffHtml binder
   * variables.
   * 
   * @param file1
   * @param file2
   * @param binder
   * @return
   */
  private String diffFiles(String file1, String file2, DataBinder binder) {
    List<String> file1Content = fileToLines(file1);
    List<String> file2Content = fileToLines(file2);

    Patch patch = DiffUtils.diff(file1Content, file2Content);

    List<String> uDiff = DiffUtils.generateUnifiedDiff(file1, file2, file1Content, patch, 10);

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
    return diffOutput.toString();
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

    String fileName = String.valueOf(revisionId) + "."
        + service.getBinder().getResultSetValue(service.getBinder().getResultSet("DOC_INFO"), "dExtension");
    String vaultFile = DirectoryLocator.computeDirectory(service.getBinder(), FileStoreProvider.R_PRIMARY) + fileName;
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
