"""Tiny generic assignment example: a heuristic and a bounded exact comparator.

No web server, solver package, bus domain or graph UI dependency.
"""
from itertools import product
from math import isfinite


def validate_input(costs, capacities):
    if not isinstance(costs, list) or not costs or not capacities or len(costs) > 8 or len(capacities) > 4:
        raise ValueError("Example supports 1..8 tasks and 1..4 resources")
    if any(type(capacity) is not int or capacity < 0 for capacity in capacities):
        raise ValueError("Nonnegative integer capacities required")
    for row in costs:
        if len(row) != len(capacities) or any(v is not None and (type(v) not in {int, float} or not isfinite(v) or v < 0) for v in row):
            raise ValueError("Costs must be nonnegative finite values or None (forbidden)")


def evaluate(costs, capacities, assignment):
    validate_input(costs, capacities)
    if len(assignment) != len(costs):
        return {"feasible": False, "cost": None, "reason": "missing_or_extra_assignment"}
    used = [0] * len(capacities)
    total = 0
    for index, resource in enumerate(assignment):
        if type(resource) is not int or not 0 <= resource < len(capacities) or costs[index][resource] is None:
            return {"feasible": False, "cost": None, "reason": "invalid_or_forbidden_resource"}
        used[resource] += 1
        total += costs[index][resource]
    if any(load > capacity for load, capacity in zip(used, capacities)):
        return {"feasible": False, "cost": None, "reason": "capacity_exceeded"}
    return {"feasible": True, "cost": total, "reason": None}


def greedy(costs, capacities):
    validate_input(costs, capacities)
    remaining = list(capacities)
    assignment = []
    for row in costs:
        choices = [r for r, cost in enumerate(row) if cost is not None and remaining[r] > 0]
        if not choices:
            return {"status": "NO_FEASIBLE_FOUND", "assignment": None, "cost": None}
        selected = min(choices, key=lambda r: (row[r], r))
        remaining[selected] -= 1
        assignment.append(selected)
    result = evaluate(costs, capacities, assignment)
    return {"status": "FEASIBLE", "assignment": assignment, "cost": result["cost"]}


def exact_tiny(costs, capacities):
    validate_input(costs, capacities)
    best = None
    for candidate in product(range(len(capacities)), repeat=len(costs)):
        result = evaluate(costs, capacities, candidate)
        if result["feasible"] and (best is None or result["cost"] < best["cost"]):
            best = {"status": "OPTIMAL_TINY_MODEL", "assignment": list(candidate), "cost": result["cost"]}
    return best or {"status": "INFEASIBLE_TINY_MODEL", "assignment": None, "cost": None}
