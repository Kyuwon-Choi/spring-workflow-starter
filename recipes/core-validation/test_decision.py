import unittest
from decision import evaluate, exact_tiny, greedy


class DecisionTest(unittest.TestCase):
    def test_hand_calculated_optimum(self):
        # Two feasible assignments: (0,1) costs 1+2=3; (1,0) costs 9+4=13.
        costs = [[1, 9], [4, 2]]
        self.assertEqual(exact_tiny(costs, [1, 1])["cost"], 3)
        self.assertEqual(greedy(costs, [1, 1])["assignment"], [0, 1])

    def test_greedy_failure_is_not_infeasibility_proof(self):
        costs = [[1, 2], [1, None]]
        self.assertEqual(greedy(costs, [1, 1])["status"], "NO_FEASIBLE_FOUND")
        exact = exact_tiny(costs, [1, 1])
        self.assertEqual(exact["assignment"], [1, 0])
        self.assertEqual(exact["cost"], 3)

    def test_baseline_can_be_feasible_but_more_expensive(self):
        costs = [[1, 2], [2, 100]]
        self.assertEqual(greedy(costs, [1, 1])["cost"], 101)
        self.assertEqual(exact_tiny(costs, [1, 1])["cost"], 4)

    def test_capacity_and_forbidden_assignment(self):
        self.assertFalse(evaluate([[0, None], [0, 1]], [1, 1], [0, 0])["feasible"])
        self.assertFalse(evaluate([[0, None]], [1, 1], [1])["feasible"])
        self.assertEqual(exact_tiny([[1], [2]], [1])["status"], "INFEASIBLE_TINY_MODEL")

    def test_shape_missing_assignment_and_invalid_numbers(self):
        self.assertFalse(evaluate([[1]], [1], [])["feasible"])
        for costs, capacities in [([[float('nan')]], [1]), ([[1, 2]], [1]), ([[1]], [-1]), ([[1]] * 9, [9])]:
            with self.assertRaises(ValueError):
                exact_tiny(costs, capacities)


if __name__ == "__main__":
    unittest.main()
