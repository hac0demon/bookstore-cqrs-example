package se.citerus.cqrs.bookstore.admin.web;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.citerus.cqrs.bookstore.admin.client.OrderClient;
import se.citerus.cqrs.bookstore.admin.client.PublisherClient;
import se.citerus.cqrs.bookstore.admin.command.CommandFactory;
import se.citerus.cqrs.bookstore.admin.web.transport.CreateBookRequest;
import se.citerus.cqrs.bookstore.admin.web.transport.OrderActivationRequest;
import se.citerus.cqrs.bookstore.admin.web.transport.RegisterPublisherRequest;
import se.citerus.cqrs.bookstore.admin.web.transport.UpdateBookPriceRequest;
import se.citerus.cqrs.bookstore.book.BookId;
import se.citerus.cqrs.bookstore.command.CommandBus;
import se.citerus.cqrs.bookstore.event.DomainEvent;
import se.citerus.cqrs.bookstore.event.DomainEventStore;
import se.citerus.cqrs.bookstore.order.book.command.CreateBookCommand;
import se.citerus.cqrs.bookstore.order.book.command.UpdateBookPriceCommand;
import se.citerus.cqrs.bookstore.publisher.PublisherContractId;
import se.citerus.cqrs.bookstore.query.OrderProjection;
import se.citerus.cqrs.bookstore.query.QueryService;

import javax.validation.Valid;
import javax.ws.rs.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("admin")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class AdminResource {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final QueryService queryService;
  private final CommandBus commandBus;
  private final OrderClient orderClient;
  private final CommandFactory commandFactory = new CommandFactory();
  private final DomainEventStore eventStore;
  private final PublisherClient publisherClient;

  public AdminResource(QueryService queryService, CommandBus commandBus, DomainEventStore eventStore, OrderClient orderClient, PublisherClient publisherClient) {
    this.queryService = queryService;
    this.commandBus = commandBus;
    this.orderClient = orderClient;
    this.eventStore = eventStore;
    this.publisherClient = publisherClient;
  }

  @GET
  @Path("orders")
  public List<OrderProjection> getOrders() {
    List<OrderProjection> projections = queryService.listOrders();
    logger.info("Returning [{}] orders", projections.size());
    return projections;
  }

  @GET
  @Path("events")
  public List<String[]> getEvents() {
    List<se.citerus.cqrs.bookstore.event.DomainEvent> allEvents = eventStore.getAllEvents();
    List<String[]> eventsToReturn = new LinkedList<>();
    for (DomainEvent event : allEvents) {
      eventsToReturn.add(new String[]{event.getClass().getSimpleName(), event.toString()});
    }
    logger.info("Returning [{}] events", eventsToReturn.size());
    return eventsToReturn;
  }

  @POST
  @Path("order-activation-requests")
  public void orderActivationRequest(@Valid OrderActivationRequest activationRequest) {
    logger.info("Activating orderId: " + activationRequest.orderId);
    orderClient.activate(activationRequest);
  }

  // TODO: Remove unused use case?
  @POST
  @Path("update-book-price-requests")
  public void updateBookPrice(@Valid UpdateBookPriceRequest updateBookPriceRequest) {
    logger.info("Updating price for book: " + updateBookPriceRequest.bookId);
    UpdateBookPriceCommand command = commandFactory.toCommand(updateBookPriceRequest);
    commandBus.dispatch(command);
  }

  @POST
  @Path("create-book-requests")
  public void bookRequest(@Valid CreateBookRequest createBookRequest) {
    BookId bookId = new BookId(createBookRequest.bookId);
    logger.info("Creating book: " + bookId);
    CreateBookCommand command = commandFactory.toCommand(bookId, createBookRequest);
    commandBus.dispatch(command);
  }

  @POST
  @Path("register-publisher-requests")
  public void registerPublisher(@Valid RegisterPublisherRequest registerPublisherRequest) {
    PublisherContractId publisherContractId = new PublisherContractId(registerPublisherRequest.publisherContractId);
    logger.info("Registering publisher: " + publisherContractId);
    publisherClient.registerPublisher(registerPublisherRequest);
  }

  // TODO: Add Simple bar chart to admin gui!
  @GET
  @Path("orders-per-day")
  public Map<LocalDate, Integer> getOrdersPerDay() {
    return queryService.getOrdersPerDay();
  }

}