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
     * Cria um registro e preenche na entidade o identificador gerado pelo banco.
     * @param entidade entidade a persistir
     * @throws SQLException se a inserção falhar
     */
    void inserir(T entidade) throws SQLException;

    /**
     * Busca um registro pelo seu identificador primário.
     * @param id identificador do registro
     * @return entidade encontrada ou null quando o registro não existir
     * @throws SQLException se a consulta falhar
     */
    T buscarPorId(int id) throws SQLException;

    /**
     * @return todos os registros cadastrados, ou lista vazia se não houver registros
     * @throws SQLException se a consulta falhar
     */
    List<T> listarTodos() throws SQLException;

    /**
     * Atualiza os dados de um registro pelo identificador da entidade.
     * @param entidade entidade com os novos dados
     * @throws SQLException se a atualização falhar
     */
    void atualizar(T entidade) throws SQLException;

    /**
     * Remove um registro do banco de dados pelo ID.
     * @param id identificador do registro
     * @throws SQLException se a exclusão falhar
     */
    void excluir(int id) throws SQLException;
}
