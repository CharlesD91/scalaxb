import java.io.File
import java.time.{OffsetDateTime, ZoneOffset}
import java.util.GregorianCalendar
import java.util.concurrent.TimeUnit

import javax.xml.datatype.DatatypeFactory
import scalaxb.compiler.Config
import scalaxb.compiler.ConfigEntry._
import scalaxb.compiler.wsdl11.Driver

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}

class WorkdayTest extends TestBase {
  override val module = new Driver

  val packageName = "generated2"
  val outdir = new File("/home/charles/IdeaProjects/scalaxbCharles/integration/src/test/scala/generated")
  val wsdlFile = new File("/home/charles/Downloads/Payroll.wsdl")

  private val config = Config.default.update(PackageNames(Map(None -> Some(packageName)))).
    update(Outdir(outdir)).
    update(GeneratePackageDir)

  // generate the beans
  // module.process(wsdlFile, config)

  "workday client works" in {

    import generated2._
    val wdPort = 32769
    val wdVersionString: String = "31.0"
    val wdTenant: String = "tenant"
    val wdUsername = "user@tenant"
    val wdPassword = "pass"
    val service: PayrollPort = new PayrollBindings with workday.WsSecuritySoap11Clients with scalaxb.DispatchHttpClientsAsync {
      override def baseAddress =
        new java.net.URI(s"http://localhost:$wdPort/workdaymocks/mockPayrollBinding/ccx/service/$wdTenant/Payroll/v$wdVersionString")

      override def username: String = wdUsername

      override def password: String = wdPassword
    }.service

    val from = OffsetDateTime.of(2016, 10, 5, 0, 0, 0, 0, ZoneOffset.UTC)
    val to = OffsetDateTime.of(2018, 10, 6, 0, 0, 0, 0, ZoneOffset.UTC)
    val fromGregorian = DatatypeFactory.newInstance.newXMLGregorianCalendar(GregorianCalendar.from(from.toZonedDateTime))
    val toGregorian = DatatypeFactory.newInstance.newXMLGregorianCalendar(GregorianCalendar.from(to.toZonedDateTime))

    val id: generated2.Pay_Run_Group_SelectionObjectIDType =
      generated2.Pay_Run_Group_SelectionObjectIDType("OK", Map("@type" -> scalaxb.DataRecord("Pay_Group_Detail_ID")))
    val group = generated2.Pay_Run_Group_SelectionObjectType(Seq(id))
    val namespace = Some("urn:com.workday/bsvc")
    val label = Some("Request_Criteria")
    val requestTypeOption: scalaxb.DataRecord[generated2.Get_Payroll_Preu45Printed_Payslips_RequestTypeOption] =
      scalaxb.DataRecord(namespace, label,
        Payroll_Preu45Printed_Payslips_Request_CriteriaType(
          fromGregorian,
          toGregorian,
          Seq(group)))

    val wdVersion = scalaxb.DataRecord(wdVersionString)
    val requestType: Get_Payroll_Preu45Printed_Payslips_RequestType = Get_Payroll_Preu45Printed_Payslips_RequestType(requestTypeOption,
      attributes = Map("@{urn:com.workday/bsvc}version" -> wdVersion))
    val headerType: Workday_Common_HeaderType = Workday_Common_HeaderType()

    // send requests
    val future: Future[generated2.Get_Payroll_Preu45Printed_Payslips_ResponseType] =
      service.get_Payroll_Preu45Printed_Payslips(requestType, headerType)

    val timeout = FiniteDuration(5, TimeUnit.SECONDS)
    val response: Get_Payroll_Preu45Printed_Payslips_ResponseType = Await.result(future, timeout)
    val refId: String = response.Response_Data.head.Employee_Name_Reference.get.ID.head.value

    refId must equalTo("jojax")
  }
}
