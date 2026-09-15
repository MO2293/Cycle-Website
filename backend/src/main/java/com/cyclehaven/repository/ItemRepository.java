package com.cyclehaven.repository;

import com.cyclehaven.entity.Item;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Catalogue queries.
 *
 * <p>Filtering is composed with {@link ItemSpecifications} rather than one
 * @Query with nullable parameters: a JPQL {@code IN} clause cannot take a null
 * collection, and Specifications let each filter be omitted entirely from the
 * generated SQL when it isn't supplied.
 */
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /** Distinct colours present in the catalogue, for building the filter sidebar. */
    @Query("SELECT DISTINCT i.colour FROM Item i ORDER BY i.colour")
    List<String> findDistinctColours();

    /**
     * Loads an item for a stock update, holding a row lock until the surrounding
     * transaction commits.
     *
     * <p>Without this, two people checking out the last bike at the same moment
     * can both read {@code quantity = 1}, both pass the stock check, and both
     * decrement — leaving quantity at -1 and two orders for one bike. That is a
     * classic read-modify-write race, and it is invisible in single-user testing.
     *
     * <p>{@code PESSIMISTIC_WRITE} issues a {@code SELECT ... FOR UPDATE}, so the
     * second transaction waits at this line until the first finishes and then
     * reads the updated quantity.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);
}
