package com.wix.pay.mercadopago.testkit


import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import com.wix.e2e.http.api.StubWebServer
import com.wix.e2e.http.client.extractors.HttpMessageExtractors._
import com.wix.e2e.http.server.WebServerFactory.aStubWebServer
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.mercadopago.model._
import com.wix.pay.mercadopago.{ErrorResponseParser, MercadopagoHelper, TokenizeRequestParser, TokenizeResponseParser}


class MercadopagoTokenizationDriver(server: StubWebServer) {
  def this(port: Int) = this(aStubWebServer.onPort(port).build)

  def start(): Unit = server.start()
  def stop(): Unit = server.stop()
  def reset(): Unit = server.replaceWith()


  def aTokenizeFor(card: CreditCard, countryCode: String): RequestCtx = {
    new RequestCtx(MercadopagoHelper.createTokenizeRequest(
      creditCard = card,
      countryCode = countryCode))
  }


  class RequestCtx(request: TokenizeRequest) {
    def returns(cardTokenId: String): Unit = {
      returns(TokenizeResponse(
        public_key = "some public key",
        card_id = "some card ID",
        luhn_validation = true,
        status = "some status",
        used_date = "some user date",
        live_mode = true,
        card_number_length = 16,
        id = cardTokenId,
        creation_date = "some creation date",
        trunc_card_number = "1234",
        security_code_length = Some(3),
        expiration_year = 2020,
        expiration_month = 12,
        last_modified_date = "some last modified date",
        cardholder = CardHolder(
          name = "some cardholder name",
          identification = Identification(
            subtype = "some subtype",
            number = "some number",
            `type` = IdentificationTypes.cpf)),
        due_date = "some due date"))
    }

    def failsWith(errorMessage: String): Unit = {
      errors(ErrorResponse(
        error = "some error",
        message = errorMessage,
        cause = List()))
    }

    def returns(response: TokenizeResponse): Unit = {
      server.appendAll {
        case HttpRequest(
          HttpMethods.POST,
          Path("/"),
          _,
          entity,
          _) if isStubbedRequest(entity) =>
            HttpResponse(
              status = StatusCodes.OK,
              entity = HttpEntity(ContentTypes.`application/json`, TokenizeResponseParser.stringify(response)))
      }
    }

    def errors(errorResponse: ErrorResponse): Unit = {
      server.appendAll {
        case HttpRequest(
          HttpMethods.POST,
          Path("/"),
          _,
          entity,
          _) if isStubbedRequest(entity) =>
            HttpResponse(
              status = StatusCodes.Forbidden,
              entity = HttpEntity(ContentTypes.`application/json`, ErrorResponseParser.stringify(errorResponse)))
      }
    }

    private def isStubbedRequest(entity: HttpEntity): Boolean = {
      val parsedRequest = TokenizeRequestParser.parse(entity.extractAsString)
      parsedRequest == request
    }
  }
}
