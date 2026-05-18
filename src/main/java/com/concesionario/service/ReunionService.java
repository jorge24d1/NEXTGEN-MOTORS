package com.concesionario.service;

import com.concesionario.model.Reunion;
import com.concesionario.repository.ReunionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReunionService {

    @Autowired
    private ReunionRepository reunionRepository;

    public List<Reunion> listarTodas() {
        return reunionRepository.findAll();
    }

    public Reunion guardar(Reunion reunion) {
        return reunionRepository.save(reunion);
    }

    public void eliminar(Long id) {
        reunionRepository.deleteById(id);
    }

    public Reunion obtenerPorId(Long id) {
        return reunionRepository.findById(id).orElse(null);
    }
}

