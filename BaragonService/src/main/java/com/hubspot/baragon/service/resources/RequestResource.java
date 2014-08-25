package com.hubspot.baragon.service.resources;

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.managers.RequestManager;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.QueuedRequestId;
import com.hubspot.baragon.service.worker.BaragonRequestWorker;

@Path("/request")
@Consumes({MediaType.APPLICATION_JSON})
@Produces(MediaType.APPLICATION_JSON)
public class RequestResource {
  private static final Log LOG = LogFactory.getLog(RequestResource.class);

  private final RequestManager manager;

  @Inject
  public RequestResource(RequestManager manager) {
    this.manager = manager;
  }

  @GET
  @Path("/{requestId}")
  public Optional<BaragonResponse> getResponse(@PathParam("requestId") String requestId) {
    return manager.getResponse(requestId);
  }

  @POST
  public BaragonResponse enqueueRequest(@Valid BaragonRequest request) {
    try {
      LOG.info(String.format("Received request: %s", request));
      return manager.enqueueRequest(request);
    } catch (Exception e) {
      LOG.error(String.format("Caught exception for %s", request.getLoadBalancerRequestId()), e);
      return BaragonResponse.failure(request.getLoadBalancerRequestId(), e.getMessage());
    }
  }

  @GET
  public List<QueuedRequestId> getQueuedRequestIds() {
    return manager.getQueuedRequestIds();
  }

  @DELETE
  @Path("/{requestId}")
  public BaragonResponse cancelRequest(@PathParam("requestId") String requestId) {
    // prevent race conditions when transitioning from a cancel-able to not cancel-able state
    synchronized (BaragonRequestWorker.class) {
      manager.cancelRequest(requestId);
      return manager.getResponse(requestId).or(BaragonResponse.requestDoesNotExist(requestId));
    }
  }
}