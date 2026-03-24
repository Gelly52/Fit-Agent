package com.itgeo.service;

import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.bean.SseTicketResponse;

public interface SseTicketService {

    SseTicketResponse createTicket(AuthenticatedUserContext authenticatedUser);

    AuthenticatedUserContext consumeTicket(String ticket);
}
