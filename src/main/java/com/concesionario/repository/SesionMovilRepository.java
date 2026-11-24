package com.concesionario.repository;

import com.concesionario.model.SesionMovil;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class SesionMovilRepository {
    private final MongoTemplate mongo;
    private boolean indiceListo;

    public SesionMovilRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    // Solo esta colección necesita un índice TTL, incluso si Mongo no autocrea índices.
    private synchronized void prepararIndice() {
        if (!indiceListo) {
            mongo.indexOps(SesionMovil.class).ensureIndex(
                    new Index().on("vence", Sort.Direction.ASC).expire(Duration.ZERO));
            indiceListo = true;
        }
    }

    public SesionMovil insert(SesionMovil sesion) {
        prepararIndice();
        return mongo.insert(sesion);
    }

    public Optional<SesionMovil> findById(String huella) {
        return Optional.ofNullable(mongo.findById(huella, SesionMovil.class));
    }

    public void deleteById(String huella) {
        mongo.remove(Query.query(Criteria.where("_id").is(huella)), SesionMovil.class);
    }
}
