package workday

import scalaxb.HttpClientsAsync

import scala.concurrent.{ExecutionContext, Future}
import xml._

// scalaxb auth
// https://groups.google.com/forum/#!topic/scalaxb/fLAMPhc5uzE
// https://github.com/alboko/aw-scalaxb-test/blob/master/src/main/scala/awtest/WsSecuritySoap11Clients.scala
trait WsSecuritySoap11Clients extends scalaxb.Soap11ClientsAsync {
  this: HttpClientsAsync =>

  def username: String

  def password: String

  override lazy val soapClient: Soap11ClientAsync = new WsSecuritySoap11Client {}

  trait WsSecuritySoap11Client extends Soap11ClientAsync {

    override def requestResponse(body: scala.xml.NodeSeq, headers: scala.xml.NodeSeq, scope: scala.xml.NamespaceBinding,
                                 address: java.net.URI, webMethod: String, action: Option[java.net.URI])
                                (implicit ec: ExecutionContext): Future[(scala.xml.NodeSeq, scala.xml.NodeSeq)] = {
      val wsSecurity =
        s"""<wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
           |<wsse:UsernameToken>
           |<wsse:Username>$username</wsse:Username>
           |<wsse:Password>$password</wsse:Password>
           |</wsse:UsernameToken>
           |</wsse:Security>""".stripMargin

      val wsSecurityHeader = XML.loadString(wsSecurity)
      super.requestResponse(body, headers ++ wsSecurityHeader, scope, address, webMethod, action)
    }
  }

}
