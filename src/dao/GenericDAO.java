package dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface genérica de persistência (Pilar de Abstração em POO).
 * Define o contrato estrito para operações de CRUD em conformidade com o edital.
 *
 * @param <T> Entidade de domínio
 */
public interface GenericDAO<T> {

    /**
     * Cria (insere) um novo registro no banco de dados (Create).
     */
    void inserir(T entidade) throws SQLException;

    /**
     * Busca um registro pelo seu identificador primário (Read).
     */
    T buscarPorId(int id) throws SQLException;

    /**
     * Retorna todos os registros cadastrados (Read).
     */
    List<T> listarTodos() throws SQLException;

    /**
     * Atualiza os dados de um registro existente no banco (Update).
     */
    void atualizar(T entidade) throws SQLException;

    /**
     * Remove um registro do banco de dados pelo ID (Delete).
     */
    void excluir(int id) throws SQLException;
}
