package com.avv

//--------------------------------------------------------
// Define los paquetes
//--------------------------------------------------------

import groovy.json.JsonOutput
import groovy.json.JsonParser
import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

//--------------------------------------------------------
// Define las funciones utilitarias
//--------------------------------------------------------

/**
 * Envia una peticion POST.
 * @param url String url del recurso del API
 * @param headers Map Encabezados de la peticion
 * @param body Map Cuerpo de la peticion
 * @param responseCode int Codigo de respuesta esperado
 * @return String JWT token | null
 */
def sendPost(url, headers, body, responseCode) {
  def response = null
  def writer = null
  try {
    response = new URL(url).openConnection()
    response.setRequestMethod("POST")
    headers.each { key, value ->
        response.setRequestProperty(key, value)
    }
    response.setDoOutput(true)
    writer = new OutputStreamWriter(response.getOutputStream())
    if (body) {
      writer.write(new groovy.json.JsonBuilder(body).toPrettyString())
    }
    writer.flush()
    writer.close()
    println response.getResponseCode()
    if (response.getResponseCode() == responseCode) {
      def json = new JsonSlurper().parseText(response.getInputStream().getText())
      writer = null
      response = null 
      return json
    } else {
      String errorMessage = new BufferedReader(new InputStreamReader(response.getErrorStream())).getText()
      println "[ERROR]:[pr-manager-library]:[sendPost]: $errorMessage"
      writer = null
      response = null
      return null
    }
  } catch (Exception e) {
      writer = null
      response = null
      println "[ERROR]:[pr-manager-library]:[sendPost]: " + e.getMessage()
      return null
  }  
}

/**
 * Envia una peticion GET.
 * @param url String url del recurso del API
 * @param headers Map Encabezados de la peticion
 * @return String JWT token | null
 */
def sendGet(url, headers) {
  def httpclient = null
  def httpGet = null
  def httpResponse = null
  def entity = null
  def response = null
  def jsonSlurper = null
  try {
    httpclient = HttpClients.createDefault()
    httpGet = new HttpGet(url)
    httpGet.setHeader(HttpHeaders.ACCEPT, headers["Accept"])
    httpGet.setHeader(HttpHeaders.AUTHORIZATION, headers["Authorization"])
    httpGet.setHeader(HttpHeaders.CONTENT_TYPE, headers["Content-Type"])
    httpGet.setHeader("X-GitHub-Api-Version", headers["X-GitHub-Api-Version"])
    httpResponse = httpclient.execute(httpGet)
    entity = httpResponse.getEntity()
    response = EntityUtils.toString(entity)
    EntityUtils.consume(entity)
    jsonSlurper = new JsonSlurper()
    def json = jsonSlurper.parseText(response)
    httpclient = null
    httpGet = null
    httpResponse = null
    entity = null
    response = null
    jsonSlurper = null
    return json
  } catch (Exception e) {
      httpclient = null
      httpGet = null
      httpResponse = null
      entity = null
      response = null
      jsonSlurper = null
      println "[ERROR]:[pr-manager-library]:[sendGet]: " + e.getMessage()
      return null
  }  
}

/**
 * Genera el JWT token.
 * @param pem String ruta y nombre del archivo pem
 * @param appID String APP ID
 * @return String JWT token | null
 */
def generateJwtToken(pemPath, appID) {
  try {
    def scriptOutput = sh(script: "python3 /root/jenkins/keys/generate_token.py ${pemPath} ${appID}", returnStdout: true).trim()
    return scriptOutput
  } catch (Exception e) {
      println "[ERROR]:[pr-manager-library]:[generateJwtToken]: " + e.getMessage()
      return null
  }
}

/**
 * Genera el token.
 * @param installID String ID de la instalacion
 * @param jwtToken String jwt token
 * @return String Token | null
 */
def getToken(installID, jwtToken) {
  try {
    def url = "https://api.github.com/app/installations/${installID}/access_tokens"
    def headers = [
        "Accept": "application/vnd.github+json",
        "Authorization": "Bearer ${jwtToken}",
        "X-GitHub-Api-Version": "2022-11-28",
        "Content-Type": "application/json"
    ]
    responseJson = sendPost(url, headers, null, 201)
    if (responseJson) {
      return responseJson.token
    }
    return null
  } catch (Exception e) {
      println "[ERROR]:[pr-manager-library]:[getToken]: " + e.getMessage()
      return null
  }
}

/**
 * Crea un Pull Request.
 * @param owner String propietario del repo
 * @param repo String nombre del repo
 * @param token String token
 * @param branch String rama origen
 * @param target String rama destino
 * @return String Token | null
 */
def createPR(owner, repo, token, branch, target) {
  try {
    url = "https://api.github.com/repos/${owner}/${repo}/pulls"
    headers = [
        "Accept": "application/vnd.github+json",
        "Authorization": "Bearer ${token}",
        "X-GitHub-Api-Version": "2022-11-28",
        "Content-Type": "application/json"
    ]
    def body = [
        "title":"Pull Request Automático","body":"Incorporación de característica","head":"${branch}","base":"${target}"
    ]
    responseJson = sendPost(url, headers, body, 201)
    if (responseJson) {
      def slots = responseJson.statuses_url.split("/")
      def commit_sha = slots[-1] 
      def response = [
        "prID": responseJson.number,
        "commit_sha": commit_sha
      ]
      return response
    }
    return null
  } catch (Exception e) {
      println "[ERROR]:[pr-manager-library]:[createPR]: " + e.getMessage()
      return null
  }   
}

def notifyCheck(owner, repo, state, sha, token) {
  try {
    headers = [
        "Accept": "application/vnd.github+json",
        "Authorization": "Bearer ${token}",
        "X-GitHub-Api-Version": "2022-11-28",
        "Content-Type": "application/json"
    ]
    def body = [
        "state":"${state}","description":"Running automated CI...","context":"continuous-integration"
    ]
    url = "https://api.github.com/repos/${owner}/${repo}/statuses/${sha}"
    res = sendPost(url, headers, body, 201)
    if (res) {
      return true
    }
    return false
  } catch (Exception e) {
      println "[ERROR]:[pr-manager-library]:[notifyCheck]: " + e.getMessage()
      return false
  }  
}

def getPRStatus(owner, repo, prID, token) {
  try {
    url = "https://api.github.com/repos/${owner}/${repo}/pulls/${prID}"
    println "Url:"
    println url
    headers = [
      "Accept": "application/vnd.github+json",
      "Authorization": "Bearer ${token}",
      "X-GitHub-Api-Version": "2022-11-28",
      "Content-Type": "application/json"
    ]
    def res = sendGet(url, headers)
    if (res) {
      def slots = null
      def commit_sha = null
      if (res.statuses_url) {
        slots = res.statuses_url.split("/")
        commit_sha = slots[-1]
      } else {
        if (res.head && res.head.sha) {
          commit_sha = res.head.sha
        } else {
          println "[WARN]:[pr-manager-library]:[getPRStatus]: No se puede obtener el SHA de:"
          println res
          return null
        }
      }
      def response = [
        "mergeable": res.mergeable,
        "commit_sha": commit_sha,
        "state": res.state
      ]
      return response
    }
    return null
  } catch (Exception e) {
      println "[ERROR]:[pr-manager-library]:[getPRStatus]: " + e.getMessage()
      return null
  }
}

def waitConflicts(owner, repo, prID, token) {
  try {
    attemps = 0
    def res = getPRStatus(owner, repo, prID, token)
    while((!res || !res.mergeable) && attemps <= 10) {
      try {
        println "[WARN]:[pr-manager-library]:[waitConflicts]: Espera ${attemps} intentos de un minuto mientras el usaurio resuelve los conflictos..."
        sleep(60)
        res = getPRStatus(owner, repo, prID, token)
      } catch (Exception ew) {
          println "[WARN]:[pr-manager-library]:[waitConflicts]: " + ew.getMessage()
      }
      attemps += 1  
    }
    return res.commit_sha
  } catch (Exception e) {
      println "[ERROR]:[pr-manager-library]:[waitConflicts]: " + e.getMessage()
      return null
  }
}

def getClosePR(owner, repo, prID, token) {
  try {
    def res = getPRStatus(owner, repo, prID, token)
    attemps = 0
    while((!res || res.state == "open") && attemps < 10) {
      try {
        println "[WARN]:[pr-manager-library]:[getClosePR]: Espera ${attemps} intentos de un minuto mientras el usaurio cierra el Pull Resquest..."
        sleep(60)
        res = getPRStatus(owner, repo, prID, token)
      } catch (Exception ew) {
          println "[WARN]:[pr-manager-library]:[getClosePR]: " + ew.getMessage()
      }
      attemps += 1  
    }
    return res.commit_sha
  } catch (Exception e) {
      println "[ERROR]:[pr-manager-library]:[getClosePR]: " + e.getMessage()
      return null
  }  
}

return this