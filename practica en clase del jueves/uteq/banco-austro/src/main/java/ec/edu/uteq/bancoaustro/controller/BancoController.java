package ec.edu.uteq.bancoaustro.controller;

import ec.edu.uteq.bancoaustro.service.ConsultaDistribuidaService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banco")
public class BancoController {
    private final ConsultaDistribuidaService service;

    public BancoController(ConsultaDistribuidaService service) {
        this.service = service;
    }

    @GetMapping("/saldo/{numero}")
    public Map<String, Object> saldo(@PathVariable String numero) {
        return service.consultarSaldo(numero);
    }

    @GetMapping("/clientes")
    public Map<String, Object> clientes() {
        return service.listarTodosLosClientes();
    }

    @PostMapping("/transferencia")
    public Map<String, Object> transferencia(@RequestBody TransferenciaRequest request) {
        return service.transferir(request.origen(), request.destino(), request.monto());
    }
}
