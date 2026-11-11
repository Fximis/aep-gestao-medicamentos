import dao.GenericDAO;
import dao.MedicamentoDAO;
import models.*;

import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Testes de domínio executáveis com Java SE, sem banco e sem JUnit. */
public class ModelosTest {
    private static int verificacoes;

    public static void main(String[] args) throws Exception {
        testarEncapsulamento();
        testarEstoque();
        testarAutonomiaEAlerta();
        testarPaciente();
        testarHerancaEDose();
        testarContratoDAO();
        System.out.println("OK: " + verificacoes + " verificações concluídas.");
    }

    private static void testarEncapsulamento() {
        for (Class<?> tipo : List.of(Usuario.class, Paciente.class, Responsavel.class, Medicamento.class, Dose.class)) {
            for (var campo : tipo.getDeclaredFields()) {
                if (!Modifier.isStatic(campo.getModifiers())) {
                    verificar(Modifier.isPrivate(campo.getModifiers()), tipo.getSimpleName() + "." + campo.getName() + " deve ser private");
                }
            }
        }
    }

    private static void testarEstoque() {
        rejeitar(IllegalArgumentException.class, () -> new Medicamento(1, "Teste", -1, 8, 1));
        Medicamento m = new Medicamento(1, "Teste", 10, 12, 1);
        rejeitar(IllegalArgumentException.class, () -> m.setQuantidadeEstoque(-1));
        verificar(m.getQuantidadeEstoque() == 10, "Setter inválido preserva o estoque");
        verificar(!m.reduzirEstoque(11), "Bloqueia consumo maior que estoque");
        verificar(!m.reduzirEstoque(0) && !m.reduzirEstoque(-1), "Bloqueia consumo não positivo");
        verificar(m.getQuantidadeEstoque() == 10, "Consumo inválido não altera estoque");
        verificar(m.reduzirEstoque(10) && m.getQuantidadeEstoque() == 0, "Permite zerar estoque");
        verificar(!m.reduzirEstoque(1), "Não reduz estoque vazio");
        m.adicionarEstoque(5);
        verificar(m.getQuantidadeEstoque() == 5, "Reposição válida");
        rejeitar(IllegalArgumentException.class, () -> m.adicionarEstoque(-1));
        rejeitar(IllegalArgumentException.class, () -> m.adicionarEstoque(0));
        m.setQuantidadeEstoque(Integer.MAX_VALUE);
        rejeitar(IllegalArgumentException.class, () -> m.adicionarEstoque(1));
        verificar(m.getQuantidadeEstoque() == Integer.MAX_VALUE, "Overflow bloqueado antes de modificar estoque");
    }

    private static void testarAutonomiaEAlerta() {
        Medicamento m = new Medicamento(1, "Teste", 60, 8, 1);
        verificar(m.calcularDiasRestantes() == 20, "60 unidades, 1 a cada 8h = 20 dias");
        m.setQuantidadePorDose(2);
        verificar(m.calcularDiasRestantes() == 10, "Recalcula ao alterar dose");
        m.setFrequenciaHoras(12);
        verificar(m.calcularDiasRestantes() == 15, "Recalcula ao alterar intervalo");
        m.setQuantidadeEstoque(20);
        verificar(m.calcularDiasRestantes() == 5 && m.verificarAlertaRecompra(), "Alerta no limite de 5 dias");
        m.adicionarEstoque(4);
        verificar(m.calcularDiasRestantes() == 6 && !m.verificarAlertaRecompra(), "Reposição retira alerta em 6 dias");
        m.reduzirEstoque(4);
        verificar(m.calcularDiasRestantes() == 5 && m.verificarAlertaRecompra(), "Consumo atualiza autonomia e alerta");
        m.setQuantidadeEstoque(0);
        verificar(m.calcularDiasRestantes() == 0 && m.verificarAlertaRecompra(), "Estoque vazio exige recompra");
        m.setFrequenciaHoras(5);
        m.setQuantidadePorDose(3);
        m.setQuantidadeEstoque(72);
        verificar(m.calcularDiasRestantes() == 5, "Intervalo que não divide 24 sem perda de precisão");
        m.setFrequenciaHoras(48);
        m.setQuantidadePorDose(1);
        m.setQuantidadeEstoque(3);
        verificar(m.calcularDiasRestantes() == 6, "Intervalo maior que um dia sem divisão por zero");
        m.setFrequenciaHoras(8);
        m.setQuantidadeEstoque(17);
        verificar(m.calcularDiasRestantes() == 5, "Dias completos, sem arredondar para cima");
        rejeitar(IllegalArgumentException.class, () -> m.setFrequenciaHoras(0));
        rejeitar(IllegalArgumentException.class, () -> m.setFrequenciaHoras(-1));
        rejeitar(IllegalArgumentException.class, () -> m.setQuantidadePorDose(0));
        rejeitar(IllegalArgumentException.class, () -> m.setQuantidadePorDose(-1));
        rejeitar(IllegalArgumentException.class, () -> new Medicamento(1, "Teste", 10, 0, 1));
        rejeitar(IllegalArgumentException.class, () -> new Medicamento(1, "Teste", 10, 8, 0));
        m.setQuantidadeEstoque(Integer.MAX_VALUE);
        m.setFrequenciaHoras(Integer.MAX_VALUE);
        verificar(m.calcularDiasRestantes() == Integer.MAX_VALUE, "Autonomia extrema não estoura int");
        verificar(new Medicamento().calcularDiasRestantes() == 0, "Construtor vazio mantém parâmetros positivos");
    }

    private static void testarPaciente() {
        Paciente p = new Paciente("Ana", "ana@example.com", "teste", "Teste");
        Medicamento m = new Medicamento(1, "Teste", 10, 12, 1);
        List<Medicamento> entrada = new ArrayList<>(List.of(m));
        p.setMedicamentos(entrada);
        entrada.clear();
        verificar(p.getMedicamentos().size() == 1, "Cópia defensiva na entrada");
        rejeitar(UnsupportedOperationException.class, () -> p.getMedicamentos().clear());
        List<Medicamento> consulta = p.getMedicamentos();
        p.adicionarMedicamento(m);
        verificar(consulta.size() == 1 && p.getMedicamentos().size() == 2, "Consulta fornece fotografia protegida da lista");
        rejeitar(IllegalArgumentException.class, () -> p.setMedicamentos(null));
        rejeitar(IllegalArgumentException.class, () -> p.setMedicamentos(Arrays.asList(m, null)));
        rejeitar(IllegalArgumentException.class, () -> p.adicionarMedicamento(null));
        verificar(p.getMedicamentos().size() == 2, "Entrada inválida preserva a lista anterior");
        p.setMedicamentos(List.of());
        verificar(p.getMedicamentos().isEmpty(), "Aceita lista vazia imutável");
    }

    private static void testarHerancaEDose() {
        Usuario paciente = new Paciente("Ana", "ana@example.com", "teste", "Teste");
        Usuario responsavel = new Responsavel("Bia", "bia@example.com", "teste", "Filha");
        verificar(paciente.obterTipo().equals("PACIENTE"), "Polimorfismo de paciente");
        verificar(responsavel.obterTipo().equals("RESPONSAVEL"), "Polimorfismo de responsável");
        verificar(paciente.autenticar("ANA@example.com", "teste"), "Autenticação usa atributos encapsulados");
        verificar(!paciente.autenticar("ana@example.com", "incorreta"), "Senha incorreta rejeitada");
        Dose dose = new Dose(1, LocalDateTime.now().minusMinutes(1));
        verificar(!dose.isStatusTomado() && dose.getHorarioConsumo() == null, "Dose começa pendente");
        dose.marcarComoTomada();
        verificar(dose.isStatusTomado() && dose.getHorarioConsumo() != null, "Confirmação registra consumo");
    }

    private static void testarContratoDAO() throws Exception {
        GenericDAO<Medicamento> dao = new MedicamentoDAO();
        verificar(dao instanceof MedicamentoDAO, "DAO concreto implementa contrato genérico");
        verificar(GenericDAO.class.isInterface(), "GenericDAO é interface");
        verificar(GenericDAO.class.getDeclaredMethods().length == 5, "Cinco operações CRUD padronizadas");
        for (String nome : List.of("inserir", "buscarPorId", "listarTodos", "atualizar", "excluir")) {
            var metodo = Arrays.stream(GenericDAO.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals(nome)).findFirst().orElseThrow();
            verificar(Arrays.asList(metodo.getExceptionTypes()).contains(SQLException.class), nome + " declara SQLException");
        }
    }

    private static void verificar(boolean condicao, String mensagem) {
        verificacoes++;
        if (!condicao) throw new AssertionError(mensagem);
    }

    private static void rejeitar(Class<? extends Throwable> tipo, Runnable acao) {
        try {
            acao.run();
        } catch (Throwable e) {
            verificar(tipo.isInstance(e), "Exceção esperada: " + tipo.getSimpleName() + ", recebida: " + e);
            return;
        }
        throw new AssertionError("Esperava " + tipo.getSimpleName());
    }
}
