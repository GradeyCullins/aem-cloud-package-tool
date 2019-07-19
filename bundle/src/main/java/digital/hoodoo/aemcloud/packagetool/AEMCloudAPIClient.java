package digital.hoodoo.aemcloud.packagetool;

import com.day.cq.commons.jcr.JcrUtil;
import com.google.gson.*;
import digital.hoodoo.aemcloud.connect.ConfigUtil;
import digital.hoodoo.aemcloud.packagetool.models.PackageModel;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import digital.hoodoo.aemcloud.packagetool.models.RepositoryModel;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;


public class AEMCloudAPIClient {

    private static Logger log = LoggerFactory.getLogger(AEMCloudAPIClient.class);

    private static final String CRLF = "\r\n";

    private ConfigUtil configUtil;

    public AEMCloudAPIClient(ResourceResolver resourceResolver) throws AEMCloudException {
        this.configUtil = new ConfigUtil(resourceResolver);
        if (this.configUtil.isInternal()) {
            String webAppURL = this.getWebAppURL();
            webAppURL = webAppURL.endsWith("/") ? webAppURL.substring(0, webAppURL.length() - 1) : webAppURL;
            this.configUtil.setAemCloudUrl(webAppURL);
        }
    }

    public List<PackageModel> getBuildPackages(String nameFilter, Map<String, RepositoryModel> buildRepositories, String buildRepositoryFilter, String buildBranchFilter, String buildBuildIdFilter) throws AEMCloudException {

        List<PackageModel> buildPackagesList = new ArrayList<PackageModel>();
        JsonArray buildPackages = this.listPackages().getAsJsonArray("buildPackages");
        for (JsonElement packageJsonElement : buildPackages) {
            String name = packageJsonElement.getAsJsonObject().get("name").getAsString();
            String repositoryTitle = packageJsonElement.getAsJsonObject().get("repository").getAsString();
            String repositoryName = JcrUtil.createValidName(repositoryTitle);
            String branch = packageJsonElement.getAsJsonObject().get("branch").getAsString();
            String buildId = packageJsonElement.getAsJsonObject().get("buildId").getAsString();
            String link = this.configUtil.getAemCloudUrl() + "/packages/build/" + buildId.substring(buildId.length() - 8) ;
            if (!repositoryName.isEmpty()) {
                RepositoryModel repositoryModel = new RepositoryModel(repositoryName, repositoryTitle, buildRepositoryFilter.equals(repositoryName));
                buildRepositories.putIfAbsent(repositoryName, repositoryModel);
            }
            if ((nameFilter.isEmpty() || name.matches(".*" + nameFilter + ".*"))
                    && (buildRepositoryFilter.isEmpty() || repositoryName.matches(".*" + buildRepositoryFilter + ".*"))
                    && (buildBranchFilter.isEmpty() || branch.matches(".*" + buildBranchFilter + ".*"))
                    && (buildBuildIdFilter.isEmpty() || buildId.matches(".*" + buildBuildIdFilter + ".*"))) {
                String path = packageJsonElement.getAsJsonObject().get("path").getAsString();
                String lastModified = packageJsonElement.getAsJsonObject().get("lastModified").getAsString().replace("Z", "");
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
                PackageModel buildPackage;
                try {
                    cal.setTime(sdf.parse(lastModified));
                    buildPackage = new PackageModel("build-package_" + path, name, cal.getTime(), path, repositoryTitle, branch, buildId, link);
				} catch (ParseException e) {
					buildPackage = new PackageModel("build-package_" + path, name, lastModified, path, repositoryTitle, branch, buildId, link);
				}
                buildPackagesList.add(buildPackage);
            }
        }
        return buildPackagesList;
    }

    public List<PackageModel> getUserPackages(String nameFilter) throws AEMCloudException {

        List<PackageModel> userPackagesList = new ArrayList<PackageModel>();
        JsonArray userPackages = this.listPackages().getAsJsonArray("userPackages");
        for (JsonElement packageJsonElement : userPackages) {
            String name = packageJsonElement.getAsJsonObject().get("name").getAsString();
            if (nameFilter.isEmpty() || name.matches(".*" + nameFilter + ".*")) {
                String path = packageJsonElement.getAsJsonObject().get("path").getAsString();
                String link = this.configUtil.getAemCloudUrl() + "/packages/user";
                String lastModified = packageJsonElement.getAsJsonObject().get("lastModified").getAsString();
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
                PackageModel userPackage;
                try {
                    cal.setTime(sdf.parse(lastModified));
                    userPackage = new PackageModel("user-package_" + path, name, cal.getTime(), path, "", "", "", link);
				} catch (ParseException e) {
					userPackage = new PackageModel("user-package_" + path, name, lastModified, path, "", "", "", link);
				}
                userPackagesList.add(userPackage);
            }
        }
        return userPackagesList;
    }

    private String getWebAppURL() throws AEMCloudException {
        String url = "/config";
        JsonObject config = executeAEMCloudApiCall(url, "GET", null);
        return config.has("webAppURL") ? config.get("webAppURL").getAsString() : "";
    }

    private JsonObject listPackages() throws AEMCloudException{
        String url = "/accounts/" + this.configUtil.getAemCloudAccount() + "/packages";
        return executeAEMCloudApiCall(url, "GET", null);
    }

    public String getPackageDownloadLink(String path) throws AEMCloudException {
        String url = "/accounts/" + this.configUtil.getAemCloudAccount() + "/packages/download";
        JsonObject packagePath = new JsonObject();
        packagePath.addProperty("packagePath", path);
        return executeAEMCloudApiCall(url, "POST", packagePath.toString()).get("downloadURL").getAsString();
    }

    public void uploadPackage(InputStream fileInputStream, String fileName) throws AEMCloudException {

        try {
            //TEMP FIX FOR PROGREXION -- START
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {

                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {

                    }
                }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            //TEMP FIX FOR PROGREXION -- END

            long start = System.currentTimeMillis();
            String boundary = "---------------------------" + start;

            URL url = new URL(this.configUtil.getAemCloudAPIUrl() + "/accounts/" + this.configUtil.getAemCloudAccount() + "/packages");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setDoOutput(true);
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            String encoded = Base64.getEncoder().encodeToString((this.configUtil.getAemCloudKeyName() + ":" + this.configUtil.getAemCloudKeyValue()).getBytes(StandardCharsets.UTF_8));
            c.setRequestProperty("Authorization", "Basic " + encoded);

            OutputStream outputStream = c.getOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

            bufferedWriter.append("--").append(boundary).append(CRLF)
                    .append("Content-Disposition: form-data; name=\"package\"; filename=\"").append(fileName).append("\"").append(CRLF)
                    .append("Content-Type: application/zip").append(CRLF)
                    .append(CRLF);

            bufferedWriter.flush();
            outputStream.flush();

            final byte[] buffer = new byte[4096];
            int bytesRead;
            while((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

            bufferedWriter.append(CRLF).append(CRLF);
            bufferedWriter.append("--").append(boundary).append("--").append(CRLF);
            bufferedWriter.flush();
            bufferedWriter.close();
            outputStream.close();

            int status = c.getResponseCode();
            c.disconnect();

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new AEMCloudException("Invalid response from API - " + e.getMessage(), e);
        }
    }

    private JsonObject executeAEMCloudApiCall(String urlString, String httpMethod, String data) throws AEMCloudException {
        try {

            //TEMP FIX FOR PROGREXION -- START
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {

                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {

                        }
                    }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            //TEMP FIX FOR PROGREXION -- END

            URL url = new URL(this.configUtil.getAemCloudAPIUrl() + urlString);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod(httpMethod);

            String encoded = Base64.getEncoder().encodeToString((this.configUtil.getAemCloudKeyName() + ":" + this.configUtil.getAemCloudKeyValue()).getBytes(StandardCharsets.UTF_8));
            c.setRequestProperty("Authorization", "Basic " + encoded);

            if (data != null) {
                c.setDoOutput(true);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(c.getOutputStream());
                outputStreamWriter.write(data);
                outputStreamWriter.flush();
            }

            final InputStream is = c.getInputStream();
            JsonParser parser = new JsonParser();
            JsonObject jsonResult = (JsonObject) parser.parse(new InputStreamReader(is));

            return jsonResult;
        } catch (IOException | JsonIOException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new AEMCloudException("Invalid response from API - " + e.getMessage(), e);
        }
    }

}
