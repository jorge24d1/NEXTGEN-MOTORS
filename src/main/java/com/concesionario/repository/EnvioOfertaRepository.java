package com.concesionario.repository;

import com.concesionario.model.EnvioOferta;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EnvioOfertaRepository extends MongoRepository<EnvioOferta, String> {}
